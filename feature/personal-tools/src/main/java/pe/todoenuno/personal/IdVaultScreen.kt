package pe.todoenuno.personal

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Bóveda local. Las fotos se guardan SOLO cifradas con AES-256-GCM en noBackupFilesDir.
 * La clave vive en Android Keystore y no se exporta.
 * El acceso a la interfaz requiere credencial del bloqueo de pantalla.
 */
class LocalIdVault(private val context:Context) {
    private val alias="todo_en_uno_private_id_aes_v1"
    private fun file(side:String):File {
        require(side=="front"||side=="back")
        return File(context.noBackupFilesDir,"protected_id_$side.bin")
    }
    fun exists(side:String)=file(side).exists()

    private fun key():SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (store.getKey(alias,null) as? SecretKey)?.let{return it}
        val generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
    fun save(side:String,raw:ByteArray) {
        require(raw.isNotEmpty() && raw.size<=12*1024*1024) {
            "La imagen no debe superar 12 MB."
        }
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key())
        val encrypted=cipher.doFinal(raw)
        val temp=File(file(side).parentFile,file(side).name+".tmp")
        temp.outputStream().use{
            it.write(cipher.iv.size)
            it.write(cipher.iv)
            it.write(encrypted)
        }
        require(temp.renameTo(file(side))) { "No se pudo guardar el archivo cifrado." }
    }
    fun load(side:String):ByteArray? {
        val source=file(side)
        if(!source.exists())return null
        val bytes=source.readBytes()
        require(bytes.size>30 && bytes.size<=13*1024*1024)
        val ivSize=bytes[0].toInt() and 0xFF
        require(ivSize==12)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(1,1+ivSize)))
        return cipher.doFinal(bytes.copyOfRange(1+ivSize,bytes.size))
    }
    fun delete(side:String)=file(side).delete()
}

@Composable
fun IdVaultScreen() {
    val context=LocalContext.current
    val activity=context as? android.app.Activity
    val keyguard=remember(context){
        context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }
    val secure=keyguard.isDeviceSecure
    val lifecycle=LocalLifecycleOwner.current
    val scope=rememberCoroutineScope()
    val vault=remember(context){LocalIdVault(context.applicationContext)}
    var unlocked by remember{mutableStateOf(false)}
    var selected by remember{mutableStateOf("front")}
    var front by remember{mutableStateOf(vault.exists("front"))}
    var back by remember{mutableStateOf(vault.exists("back"))}
    var bitmap by remember{mutableStateOf<Bitmap?>(null)}
    var status by remember{mutableStateOf("")}
    fun lock() {
        unlocked=false
        bitmap?.recycle()
        bitmap=null
    }
    DisposableEffect(activity,lifecycle) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val observer=LifecycleEventObserver{_,event ->
            if(event==Lifecycle.Event.ON_STOP)lock()
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose {
            lock()
            lifecycle.lifecycle.removeObserver(observer)
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    fun show(side:String) {
        selected=side
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val data=vault.load(side)?:return@withContext null
                    try {
                        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                        BitmapFactory.decodeByteArray(data,0,data.size,bounds)
                        var sampling=1
                        while(maxOf(bounds.outWidth,bounds.outHeight)/sampling>1600)sampling*=2
                        val opts=BitmapFactory.Options().apply{inSampleSize=sampling}
                        BitmapFactory.decodeByteArray(data,0,data.size,opts)
                    } finally {data.fill(0)}
                }
            }.onSuccess {bmp ->
                bitmap?.recycle()
                bitmap=bmp
                status=if(bmp==null)"No hay imagen guardada." else ""
            }.onFailure{status="No se pudo abrir la imagen cifrada."}
        }
    }
    val verify=rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){result ->
        if(result.resultCode==android.app.Activity.RESULT_OK) {
            unlocked=true
            show(selected)
        } else status="No se autorizó el acceso."
    }
    val pick=rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ){uri ->
        if(uri!=null && unlocked) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val raw=context.contentResolver.openInputStream(uri)?.use{input ->
                        val output=ByteArrayOutputStream()
                        val chunk=ByteArray(8192)
                        while(true) {
                            val count=input.read(chunk)
                            if(count<0)break
                            require(output.size()+count<=12*1024*1024){"La imagen es demasiado grande."}
                            output.write(chunk,0,count)
                        }
                        output.toByteArray()
                    }?:error("No se pudo abrir la foto.")
                    try {vault.save(selected,raw)} finally{raw.fill(0)}
                }
            }.onSuccess {
                front=vault.exists("front")
                back=vault.exists("back")
                status="Foto guardada cifrada, únicamente en este teléfono."
                show(selected)
            }.onFailure{status="No se pudo guardar: ${it.message}"}
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)){
        Text("Bóveda de DNI",style=MaterialTheme.typography.headlineSmall,
            fontWeight=FontWeight.Bold)
        Text("Frente y reverso del DNI. Guardado privado con cifrado AES-GCM y sin copias en la nube.",
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(!secure){
            Text("Primero configura PIN, patrón o contraseña de bloqueo del teléfono para habilitar esta función.",
                color=MaterialTheme.colorScheme.error)
        } else if(!unlocked) {
            Button(onClick={
                val intent=keyguard.createConfirmDeviceCredentialIntent(
                    "Verificar identidad","Desbloquea el teléfono para acceder a tu DNI")
                if(intent!=null)verify.launch(intent)
                else status="No se pudo iniciar la verificación del dispositivo."
            }) {Text("Desbloquear bóveda")}
        } else {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={show("front")}){Text("Frente")}
                Button(onClick={show("back")}){Text("Reverso")}
                TextButton(onClick={lock()}){Text("Bloquear")}
            }
            Card(Modifier.fillMaxWidth()){
                val current=bitmap
                if(current!=null && !current.isRecycled) {
                    Image(bitmap=current.asImageBitmap(),contentDescription="Foto del DNI",
                        contentScale=ContentScale.Fit,
                        modifier=Modifier.fillMaxWidth().height(270.dp))
                } else Text("No hay vista previa.",Modifier.padding(20.dp))
            }
            Button(onClick={pick.launch("image/*")}) {
                Text("Elegir foto de ${if(selected=="front")"frente" else "reverso"}")
            }
            TextButton(onClick={
                vault.delete(selected)
                front=vault.exists("front")
                back=vault.exists("back")
                lock()
                status="Imagen eliminada."
            }) {Text("Eliminar foto seleccionada")}
        }
        Text("Guardado: frente ${if(front)"sí" else "no"} · reverso ${if(back)"sí" else "no"}")
        if(status.isNotBlank())Text(status,color=MaterialTheme.colorScheme.primary)
        Text("El contenido se borra al desinstalar la app o al eliminar sus datos. Una fotografía no reemplaza el DNI físico. El sistema o un dispositivo comprometido podrían afectar la privacidad.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
