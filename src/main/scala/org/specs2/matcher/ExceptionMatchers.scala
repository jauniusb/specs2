package org.specs2
package matcher
import control.Exceptions._

trait ExceptionMatchers {
  /**
   * return a matcher which will be ok if an exception of that type is thrown
   */
  def throwA[E <: Throwable](implicit m: ClassManifest[E]) = new ExceptionClassMatcher(m.erasure)
  /**
   * @see throwException description
   */
  def throwA[E <: Throwable](e: E) = new ExceptionMatcher(e)
  /**
   * return a matcher which will be ok if an exception of that type is thrown
   */
  def throwAn[E <: Throwable](implicit m: ClassManifest[E]) = throwA[E](m)
  /**
   * Alias for throwA(new Exception)
   * @see throwException description
   */
  def throwAn[E <: Throwable](e: E) = throwA(e)
  /**
   * Exception matcher checking the type of a thrown exception.
   */
  class ExceptionClassMatcher(klass: Class[_]) extends Matcher[Any] {
    def apply[S <: Any : Expectable](value: =>S) = {
      check(value, implicitly[Expectable[S]], (e: Throwable) => classType(e))
    }
    def like[T](f: =>PartialFunction[Throwable, Boolean]) = new Matcher[T] {
      def apply[S <: T : Expectable](v: =>S) = {
    	check(value, implicitly[Expectable[S]], (e: Throwable) => classType(e) && f(e))
      }
    }
    private val classType = (e: Throwable) => klass.isAssignableFrom(e.getClass)  
    private def check[T](value: =>Any, expected: Expectable[T], f: Throwable => Boolean) = {
      checkExceptionValue(value, expected, f, asString(klass))
    }
    private def asString(exception: Any) = {
      exception match {
        case e: Class[_] => e.toString.replaceFirst("class ", "")
        case ex: Throwable => ex.getClass.getName + ": " + ex.getMessage
        case other => other.toString
      }
    }
  }
  /**
   * This matchers matches exception instances.
   * @see throwA
   */
  class ExceptionMatcher[E <: Throwable](exception: E) extends Matcher[Any] {
    def apply[S <: Any : Expectable](value: =>S) = {
      check(value, implicitly[Expectable[S]], (e: Throwable) => classAndMessage(e))
    }
    def like(f: =>PartialFunction[E, Boolean]) = new Matcher[Any] {
      def apply[S <: Any : Expectable](value: =>S) = 
    	check(value, implicitly[Expectable[S]], (e: Throwable) => classAndMessage(e) && f(e.asInstanceOf[E]))
    }
    private val classAndMessage = (e: Throwable) => exception.getClass == e.getClass && exception.getMessage == e.getMessage 
    private def check[T](value: =>Any, expected: Expectable[T], f: Throwable => Boolean) = {
      checkExceptionValue(value, expected, f, exception.toString)
    }
  }
  private def checkExceptionValue[T](value: =>Any, expected: Expectable[T], f: Throwable => Boolean, expectedAsString: String) = {
      checkException(value, expected, 
		  f,
		  (e: Throwable) => "Got the exception " + e, 
		  (e: Throwable) => "Expected: "+ expectedAsString + ". Got: " + e + " instead",
		  "Got the exception " + expectedAsString, 
	 	  "Expected: "+ expectedAsString + ". Got nothing")
  }
  private def checkException[T](value: =>Any, expected: Expectable[T], f: Throwable => Boolean,
		  someOk: Throwable => String, someKo: Throwable => String,
		  noneOk: String, noneKo: String) = {
    getException(value) match {
	  case Some(e) => Matcher.result(f(e), someOk(e), someKo(e), expected)
	  case None    => Matcher.result(false, noneOk, noneKo, expected)
    }
  }
  
  private def message(exception: Any) = {
    exception match {
      case e: Class[_] => e.toString.replaceFirst("class ", "")
      case ex: Throwable => ex.getClass.getName + ": " + ex.getMessage
      case other => other.toString
    }
  }
  /** 
   * Evaluates a value and return any exception that is thrown
   * In the case of the use of the like method (throwAn[Exception].like) the value
   * will be an Expectable encapsulating the real value which needs to be evaluated
   */
  private def getException[E <: Throwable](value: =>Any): Option[Throwable] = {
    catchAll { 
      value match { 
    	case e: Expectable[_] => e.value 
    	case _ => value 
      } 
    }(identity).left.toOption
  }
}