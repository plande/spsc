package spsc

object Algebra {
  
  def shallowEq(e1: CFG, e2: CFG) = e1.kind == e2.kind && e1.name == e2.name
  
  def subst(term: Term, m: Map[Var, Term]): Term = term match {
    case v: Var     => m.getOrElse(v, v)
    case e: CFG => e.replaceArgs(e.args.map(subst(_, m)))
  }
  
  def equiv(t1: Term, t2: Term): Boolean = inst(t1, t2) && inst(t2, t1)
  
  def inst(t1: Term, t2: Term): Boolean = findSubst(t1, t2) != null
  
  def findSubst(t1: Term, t2: Term): Map[Var, Term] = {
    val map = scala.collection.mutable.Map[Var, Term]()
    def walk(t1: Term, t2: Term): Boolean = (t1, t2) match {
      case (v1: Var, _) => map.getOrElse(v1, t2) == (map+(v1 -> t2))(v1)
      case (e1: CFG, e2:CFG) if shallowEq(e1, e2) =>
        List.forall2(e1.args, e2.args)(walk)
      case _ => false
    }
    if (walk(t1, t2)) Map(map.toList:_*).filter{case (k, v) => k != v} else null
  }
  
  def vars(t: Term): List[Var] = t match {
    case v: Var   => (List(v))
    case e: CFG => (List[Var]() /: e.args) {(vs, exp) =>  vs ++ (vars(exp) -- vs)}
  }
  
  def freshVar(x: AnyRef = null) = {i += 1; Var("v" + i)}; private var i = 0;
  
  def trivial(expr: Term): Boolean = expr match {
    case FCall(_, _) => false
    case GCall(_, _) => false
    case _ => true
  }
}