package spsc

case class Gen(t: Term, m1: Map[Var, Term], m2: Map[Var, Term])
object Algebra {
  def sub(term: Term, map: Map[Var, Term]): Term = term match {
    case v: Var       => map.getOrElse(v, v)
    case Ctr(n, vs)   => Ctr(n,  vs.map(sub(_, map)))
    case FCall(n, vs) => FCall(n, vs.map(sub(_, map)))
    case GCall(n, vs) => GCall(n, vs.map(sub(_, map)))
  }
  def equiv(t1: Term, t2: Term): Boolean = inst(t1, t2) && inst(t2, t1)
  def inst(t1: Term, t2: Term): Boolean = findSub(t1, t2) != null
  def findSub(t1: Term, t2: Term): Map[Var, Term] = {
    val map = scala.collection.mutable.Map[Var, Term]()
    def walk(t1: Term, t2: Term): Boolean = (t1, t2) match {
      case (v1: Var, _) => map.getOrElse(v1, t2) == (map+(v1 -> t2))(v1) 
      case (Ctr(n1, xs),  Ctr(n2, ys))    => n1 == n2 && List.forall2(xs, ys)(walk)
      case (FCall(n1, xs), FCall(n2, ys)) => n1 == n2 && List.forall2(xs, ys)(walk)
      case (GCall(n1, xs), GCall(n2, ys)) => n1 == n2 && List.forall2(xs, ys)(walk)
      case _ => false
    }
    if (walk(t1, t2)) Map(map.toList:_*).filter{case (k, v) => k != v} else null
  }
  def vars(t: Term): List[Var] = t match {
    case v: Var   => (List(v))
    case c: Ctr   => (List[Var]() /: c.args) {_ union vars(_)}
    case f: FCall => (List[Var]() /: f.args) {_ union vars(_)}
    case g: GCall => (List[Var]() /: g.args) {_ union vars(_)}
  }
  def he_*(t1: Term, t2: Term): Boolean = he(t1, t2) && b(t1) == b(t2)
  def he(t1: Term, t2: Term): Boolean = heByDiving(t1, t2) || heByCoupling(t1, t2)  
  private def heByDiving(t1: Term, t2: Term): Boolean = t2 match {
    case (Ctr(_, args))  => args exists (he(t1, _))
    case (FCall(_, args)) => args exists (he(t1, _))
    case (GCall(_, args)) => args exists (he(t1, _))
    case _ => false
  }
  private def heByCoupling(t1: Term, t2: Term): Boolean = (t1, t2) match {
    case (Ctr(n1, args1),  Ctr(n2, args2))    if n1 == n2 => List.forall2(args1, args2)(he)
    case (FCall(n1, args1), FCall(n2, args2)) if n1 == n2 => List.forall2(args1, args2)(he)
    case (GCall(n1, args1), GCall(n2, args2)) if n1 == n2 => List.forall2(args1, args2)(he)
    case (Var(_), Var(_)) => true
    case _ => false
  }
  def msg(t1: Term, t2: Term): Gen = {
    val v = nv()
    var g = Gen(v, Map(v -> t1), Map(v -> t2))
    var exp = g.t
    do {exp = g.t; g = commonSub(commonFun(g))} while (exp != g.t)
    g
  }
  def commonFun(g: Gen): Gen = {
    for (v <- g.m1.keys) (g.m1(v), g.m2(v)) match {
      case (Ctr(n1, args1), Ctr(n2, args2)) if n1 == n2 => {
        val vs = args1.map(nv)
        val t = sub(g.t, Map(v -> Ctr(n1, vs)))
        return Gen(t, (g.m1 - v) ++ vs.zip(args1), (g.m2 - v) ++ vs.zip(args2))
      }
      case (FCall(n1, args1), FCall(n2, args2)) if n1 == n2 => {
        val vs = args1.map(nv)
        val t = sub(g.t, Map(v -> FCall(n1, vs)))
        return Gen(t, (g.m1 - v) ++ vs.zip(args1), (g.m2 - v) ++ vs.zip(args2))
      }
      case (GCall(n1, args1), GCall(n2, args2)) if n1 == n2 => {
        val vs = args1.map(nv)
        val t = sub(g.t, Map(v -> GCall(n1, vs)))
        return Gen(t, (g.m1 - v) ++ vs.zip(args1), (g.m2 - v) ++ vs.zip(args2))
      }
      case _ =>
    }
    g
  }
  def commonSub(gen: Gen): Gen = {
    for ((v1, e1) <- gen.m1; (v2, e2) <- gen.m1)
      if ((v1 != v2 && e1 == e2) && (gen.m2(v1) == gen.m2(v2)))
        return Gen(sub(gen.t, Map(v1 -> v2)), gen.m1 - v1, gen.m2 - v1)
    gen
  }
  def nv(x: AnyRef) = {i += 1; Var("v" + i)}; private var i = 0;
  private def b(t: Term): Int = t match {
    case GCall(_, args) => b(args(0))
    case Var(_) => 1
    case _ => 0
  }
}