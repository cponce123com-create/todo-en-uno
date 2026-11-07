package pe.todoenuno.education

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class EducationMathTest {
    @Test fun physicalFormulaCalculations() {
        assertEquals(10.0,PhysicsCatalog.solve("speed",100.0,10.0).getOrThrow(),1e-9)
        assertEquals(24.0,PhysicsCatalog.solve("ohm",2.0,12.0).getOrThrow(),1e-9)
        assertEquals(49.05,PhysicsCatalog.solve("potential",1.0,5.0).getOrThrow(),1e-9)
        assertTrue(PhysicsCatalog.solve("density",2.0,0.0).isFailure)
        assertEquals(12,PhysicsCatalog.formulas.size)
    }
    @Test fun notableTrianglesRatios() {
        val half=TriangleMath.solve(NotableTriangle.TRIANGLE_30,KnownSide.SMALL,5.0)
        assertEquals(10.0,half.hyp,1e-8)
        assertEquals(5.0*sqrt(3.0),half.large,1e-8)
        val diagonal=TriangleMath.solve(NotableTriangle.TRIANGLE_45,KnownSide.HYP,sqrt(2.0))
        assertEquals(1.0,diagonal.small,1e-8)
        assertEquals(1.0,diagonal.large,1e-8)
        assertEquals(5.0,TriangleMath.solve(NotableTriangle.TRIANGLE_345,KnownSide.SMALL,3.0).hyp,1e-8)
    }
    @Test(expected=IllegalArgumentException::class)
    fun rejectsNegativeLengths(){TriangleMath.solve(NotableTriangle.TRIANGLE_30,KnownSide.SMALL,-2.0)}
}
