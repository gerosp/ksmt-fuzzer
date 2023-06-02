import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverException
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.KSolverUnsupportedFeatureException
import io.ksmt.solver.yices.KYicesSolver
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.solver.bitwuzla.KBitwuzlaSolver
import io.ksmt.solver.cvc5.KCvc5Solver
import io.ksmt.sort.KBoolSort
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.math.min


val solverNames = mutableMapOf<KSolver<*>, String>()
fun main() {
    val randomExpressionGenerator = RandomExpressionGenerator()
    val ctx = KContext()
    with(ctx) {
        var expressions = randomExpressionGenerator.generate(
            1000,
            ctx,
            generatorFilter = RandomExpressionGenerator.excludeSomeSorts,
            random = Random(1)
        )
        expressions = expressions.takeLast(100).filter { it.sort is KBoolSort } as List<KExpr<KBoolSort>>

        var status: KSolverStatus
        for (expression in expressions) {
            val results = mutableListOf<KSolverStatus>()
            println("Выражение: ${expression.sort} ${expression.stringRepr.substring(0,min(50,expression.stringRepr.length)).filter{it!='\n'}}...")
            val solvers = listOf( // TODO: Не пересоздавать все каждый раз
                KCvc5Solver(this).apply { name = "cvc5" },
                KZ3Solver(this).apply { name = "Z3" },
                KYicesSolver(this).apply { name = "Yices" },
                KBitwuzlaSolver(this).apply { name = "Bitwuzla" }
            )
            for (solver in solvers) {
//                status = KSolverStatus.UNKNOWN

                try {
                    status = testSolver(solver, listOf(expression))
                    println("${solver.name}: $status")
                    results.add(status)
                } catch (e: KSolverUnsupportedFeatureException) {
                    //TODO: Обработка ошибок
                    println("${solver.name}: Теория не поддерживается")
                }catch(e: KSolverException){
                    println("${solver.name}: Произошла ошибка  " + e.message)
                }
            }
            if(KSolverStatus.SAT in results && KSolverStatus.UNSAT in results){
                println("Найдено несоотвествие\n\n\n")
            }else{
                println("Результаты работы одинаковые")
            }
        }
    }
}

fun testSolver(kSolver: KSolver<*>, expressions: List<KExpr<KBoolSort>>): KSolverStatus {
    kSolver.use { solver ->
        for (expr in expressions) {
            solver.assert(expr)
        }
        return solver.check(timeout = 1.seconds)
    }
}


var KSolver<*>.name: String
    get() = solverNames[this] ?: ""
    set(value) {
        solverNames[this] = value
    }
