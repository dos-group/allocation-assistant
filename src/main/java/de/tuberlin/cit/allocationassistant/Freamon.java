package de.tuberlin.cit.allocationassistant;

import akka.actor.*;
import com.typesafe.config.ConfigFactory;

import java.io.Serializable;

/**
 * Sends messages to the Freamon MonitorMasterActor
 */
public class Freamon extends UntypedActor {

    private static final ActorRef instance = getActor();

    private static ActorRef getActor() {
        // TODO pass application.conf path in config.xml
        final com.typesafe.config.Config config = ConfigFactory
            .parseString("akka.remote.netty.tcp.hostname=" + Config.getInstance().getConfigItem("akkaHost")
                    + "\nakka.remote.netty.tcp.port=" + Config.getInstance().getConfigItem("akkaPort"))
            .withFallback(ConfigFactory.load());

        final ActorSystem actorSystem = ActorSystem.create("allocationAssistantSystem", config);

        return actorSystem.actorOf(Props.create(Freamon.class), "freamonSender");
    }

    private final ActorSelection freamonMaster = getFreamonMasterActor();

    private ActorSelection getFreamonMasterActor() {
        final Address masterSystemPath = new Address("akka.tcp",
            Config.getInstance().getConfigItem("freamonMasterSystemName"),
            Config.getInstance().getConfigItem("freamonMasterHost"),
            Integer.parseInt(Config.getInstance().getConfigItem("freamonMasterPort")));

        final String masterActorPath = masterSystemPath.toString() + "/user/"
            + Config.getInstance().getConfigItem("freamonMasterActorName");

        return getContext().actorSelection(masterActorPath);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        freamonMaster.forward(message, getContext());
    }

    public static void onSubmit(String jobID) {
        // we could use this to add not-yet-started jobs to the db
    }

    public static void onStart(String jobID, long startTime) {
        instance.tell(new Serializable[]{"jobStarted", jobID, startTime}, instance);
    }

    public static void onStop(String jobID, long stopTime) {
        instance.tell(new Serializable[]{"jobStopped", jobID, stopTime}, instance);
    }
}
