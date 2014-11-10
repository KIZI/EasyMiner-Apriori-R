package cz.vse.easyminer.miner

trait MinerProcess {
  
  def mine(mt: MinerTask) : MinerResult
  
}
