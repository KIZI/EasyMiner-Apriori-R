package cz.vse.easyminer.miner

abstract class MinerProcess {

  self: MinerTaskValidator =>

  final def mine(mt: MinerTask) = {
    validate(mt)
    innerMine(mt)
  }

  protected def innerMine(mt: MinerTask): Seq[ARule]

}
