package devnull.rest.helpers

import unfiltered.directives.FilterDirective
import unfiltered.directives.Result.{Failure, Success}
import unfiltered.request.HttpRequest
import unfiltered.response.ResponseFunction

object DirectiveHelper {

  case class when[A](f: PartialFunction[HttpRequest[Any], A]) {
    def orElse[R](fail:ResponseFunction[R]) =
      new FilterDirective[Any, ResponseFunction[R], A](r =>
        if(f.isDefinedAt(r)) Success(f(r)) else Failure(fail),
        _ => Failure(fail)
      )
    def orElse[R](elseValue: A) =
      new FilterDirective[Any, ResponseFunction[R], A](r =>
        if (f.isDefinedAt(r)) Success(f(r)) else Success(elseValue),
        _ => Success(elseValue)
      )
  }

  case class some[A](fs: List[PartialFunction[HttpRequest[Any], A]]) {

    def orElse[R](fail: ResponseFunction[R]) =
      new FilterDirective[Any, ResponseFunction[R], A](r =>
        fs.find(f => f.isDefinedAt(r)) match {
          case Some(f) => Success(f(r))
          case None => Failure(fail)
        },
        _ => Failure(fail)

      )
  }

}
