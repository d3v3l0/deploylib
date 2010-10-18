package deploylib
package mesos

import edu.berkeley.cs.scads.comm._

import net.lag.logging.Logger

class RemoteExperimentScheduler extends ExperimentScheduler with MessageReceiver {
  val logger = Logger()
  val remoteService = RemoteActor("mesos-ec2", 9001, ActorNumber(0))
  implicit val returnAddress = MessageHandler.registerService(this)

  def scheduleExperiment(processes: Seq[JvmProcess]): Unit = {
    remoteService ! RunExperiment(processes.toList)
  }

  def receiveMessage(src: Option[RemoteActorProxy], msg: MessageBody): Unit = {
    logger.info("Received %s from %s", msg, src)
  }
}

class ExperimentService(mesosMaster: String) extends LocalExperimentScheduler("ExperimentDaemon", mesosMaster) with ServiceHandler[ExperimentOperation] {
  def startup: Unit = {
    RClusterZoo.root.getOrCreate("scads/experimentService").data = remoteHandle.toBytes 
  }

  def shutdown: Unit = null

  def process(src: Option[RemoteActorProxy], msg: ExperimentOperation) = msg match {
    case RunExperiment(processes) => scheduleExperiment(processes)
  }
}

object ExperimentDaemon extends optional.Application {
  def main(mesosMaster: Option[String]): Unit = {
    System.loadLibrary("mesos")
    new ExperimentService(mesosMaster.getOrElse("1@" + java.net.InetAddress.getLocalHost.getHostName + ":5050"))
  }
}