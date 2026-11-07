package pe.todoenuno.peru

import org.junit.Assert.*
import org.junit.Test

class IgpServiceTest {
    @Test fun decodeOfficialFields(){
        val data="""{"features":[{"attributes":{"magnitud":4.4,"profundidad":32.5,"fecha_local":"05/11/2026","hora_local":"14:20","referencia":"Arequipa","latitud":-16.4,"longitud":-71.5}}]}"""
        val report=IgpService.decode(data).single()
        assertEquals(4.4,report.magnitude!!,1e-9)
        assertEquals("Arequipa",report.reference)
        assertEquals(-16.4,report.latitude!!,1e-9)
    }
}
