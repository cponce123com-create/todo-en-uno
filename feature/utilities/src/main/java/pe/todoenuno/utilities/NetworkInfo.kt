package pe.todoenuno.utilities

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NetworkInfoScreen(){
    val context=LocalContext.current
    val manager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    var refresh by remember { mutableIntStateOf(0) }
    val state=remember(refresh){
        runCatching{
            val active=manager.activeNetwork
            val caps=manager.getNetworkCapabilities(active)
            val type=when{
                caps==null -> "Sin conexión"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)->"Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)->"Datos móviles"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)->"Ethernet"
                else->"Otra conexión"
            }
            val verified=caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true
            val dns=manager.getLinkProperties(active)?.dnsServers?.joinToString(", "){it.hostAddress ?: "?"}
                ?: "No disponible"
            Triple(type,verified,dns)
        }.getOrElse{Triple("No disponible",false,"No disponible")}
    }
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        Text("Información de red",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Transporte: ${state.first}",style=MaterialTheme.typography.titleMedium)
        Text("Red validada por Android: ${if(state.second)"Sí" else "No"}")
        Text("Servidores DNS: ${state.third}")
        Button(onClick={refresh++}){Text("Actualizar datos")}
        Text("No realizamos escaneos de redes, pruebas de penetración ni acceso al tráfico de otras apps.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
