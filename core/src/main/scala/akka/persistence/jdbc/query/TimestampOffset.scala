/*
 * Copyright (C) 2020 Lightbend Inc. <https://www.lightbend.com>
 */
package akka.persistence.jdbc.query

import akka.persistence.query.Offset

// FIXME maybe include this in Akka?

final case class TimestampOffset(timestamp: java.sql.Timestamp) extends Offset
