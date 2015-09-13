package common

import scala.collection.mutable.ListBuffer

case class AssignWork (difficulty: Int, worksize: Int)
case class Bitcoin (input: String, hash: String)
case class BitcoinList (bitcoins : ListBuffer[Bitcoin])