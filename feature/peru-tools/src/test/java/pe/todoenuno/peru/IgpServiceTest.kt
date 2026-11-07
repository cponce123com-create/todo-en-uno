package pe.todoenuno.peru

import org.junit.Assert.*
import org.junit.Test

class IgpServiceTest {
    @Test fun chronologicalOrderIsBasedOnActualDateNotStringSorting() {
        val older=QuakeReport(3.0,20.0,"31/10/2026","23:00","Antiguo",null,null)
        val newer=QuakeReport(4.0,15.0,"01/11/2026","01:00","Reciente",null,null)
        assertEquals(listOf(newer,older),IgpService.newestFirst(listOf(older,newer)))
    }

    @Test fun newestEventAfterThirtyOlderRowsIsNotDropped() {
        val rows=(1..31).joinToString(",") { day ->
            """{"attributes":{"fecha_local":"${day.toString().padStart(2,'0')}/10/2026","hora_local":"12:00","referencia":"Evento $day"}}"""
        }
        val decoded=IgpService.decode("""{"features":[$rows]}""")
        assertEquals(30,decoded.size)
        assertEquals("Evento 31",decoded.first().reference)
        assertEquals("Evento 2",decoded.last().reference)
    }

    @Test fun historicalFieldsUsePeruTimeAndSkipUndatedRows() {
        val json="""{"features":[{"attributes":{"fechaevento":null,"magnitud":8}},{"attributes":{"fechaevento":1794036436000,"magnitud":3.8,"prof":38,"ref":"Tumbes","lat":-3.48,"lon":-79.76}}]}"""
        val report=IgpService.decode(json).single()
        assertEquals("07/11/2026",report.date)
        assertEquals("02:27:16",report.time)
        assertEquals(38.0,report.depthKm!!,0.0)
        assertEquals(-3.48,report.latitude!!,0.0)
        assertEquals("Tumbes",report.reference)
    }

    @Test fun decodeOfficialFields(){
        val data="""{"features":[{"attributes":{"magnitud":4.4,"profundidad":32.5,"fecha_local":"05/11/2026","hora_local":"14:20","referencia":"Arequipa","latitud":-16.4,"longitud":-71.5}}]}"""
        val report=IgpService.decode(data).single()
        assertEquals(4.4,report.magnitude!!,1e-9)
        assertEquals("Arequipa",report.reference)
        assertEquals(-16.4,report.latitude!!,1e-9)
    }
}
