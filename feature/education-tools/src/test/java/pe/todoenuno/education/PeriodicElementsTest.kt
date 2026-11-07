package pe.todoenuno.education

import org.junit.Assert.*
import org.junit.Test

class PeriodicElementsTest {
    @Test fun all118Unique() {
        assertEquals(118,PeriodicElements.all.size)
        assertEquals(118,PeriodicElements.all.map{it.atomicNumber}.toSet().size)
        assertEquals(118,PeriodicElements.all.map{it.symbol}.toSet().size)
    }
    @Test fun knownElements() {
        assertEquals("H",PeriodicElements.byNumber(1)!!.symbol)
        assertEquals("Fe",PeriodicElements.byNumber(26)!!.symbol)
        assertEquals("Oganesón",PeriodicElements.byNumber(118)!!.name)
        assertEquals(8,PeriodicElements.find("Oxígeno").single().atomicNumber)
        assertEquals(79,PeriodicElements.find("Au").single().atomicNumber)
    }
    @Test fun periodicPosition() {
        val gold=PeriodicElements.byNumber(79)!!
        assertEquals(11,gold.group)
        assertEquals(6,gold.period)
        assertEquals(ElementFamily.NOBLE,PeriodicElements.byNumber(10)!!.family)
    }
}
