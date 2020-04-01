/*
 * Copyright (C) 2020 Lightbend Inc. <https://www.lightbend.com>
 */
package akka.persistence.jdbc.journal.dao

import slick.util.MacroSupport.macroSupportInterpolation
import slick.ast.Node
import slick.ast.TableNode
import slick.compiler.CompilerState
import slick.jdbc.JdbcProfile

trait SpannerProfile extends JdbcProfile {
  override def quoteIdentifier(id: String): String = id
  override def quoteTableName(t: TableNode): String = t.tableName

  override def createQueryBuilder(n: Node, state: CompilerState): QueryBuilder = new QueryBuilder(n, state)

  class QueryBuilder(tree: Node, state: CompilerState) extends super.QueryBuilder(tree, state) {

    override protected def buildFetchOffsetClause(fetch: Option[Node], offset: Option[Node]) = (fetch, offset) match {
      case (Some(t), Some(d)) => b"\nlimit $t offset $d"
      case (Some(t), None)    => b"\nlimit $t"
      case (None, Some(d))    => b"\nlimit -1 offset $d"
      case _                  =>
    }

  }
}

object SpannerProfile extends SpannerProfile
