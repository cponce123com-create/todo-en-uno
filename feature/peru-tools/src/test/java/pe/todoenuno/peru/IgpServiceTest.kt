package pe.todoenuno.peru

import org.junit.Assert.*
import org.junit.Test

class IgpServiceTest {
    @Test fun chronologicalOrderIsBasedOnActualDateNotStringSorting() {
        val older=QuakeReport(3.0,20.0,"31/10/2026","23:00","Antiguo",null,null)
        val newer=QuakeReport(4.0,15.0,"01/11/2026","01:00","Reciente",null,null)
        assertEquals(listOf(newer,older),IgpService.newestFirst(listOf(older,newer)))
    }

    @Test fun decodeOfficialFields(){
        val data="""{"features":[{"attributes":{"magnitud":4.4,"profundidad":32.5,"fecha_local":"05/11/2026","hora_local":"14:20","referencia":"Arequipa","latitud":-16.4,"longitud":-71.5}}]}"""
        val report=IgpService.decode(data).single()
        assertEquals(4.4,report.magnitude!!,1e-9)
        assertEquals("Arequipa",report.reference)
        assertEquals(-16.4,report.latitude!!,1e-9)
    }
}
