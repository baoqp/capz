package example;

import com.capz.core.Capz;
import com.capz.core.eventbus.EventBus;
import com.capz.core.eventbus.MessageConsumer;
import com.capz.core.impl.CapzImpl;

/**
 * @author Bao Qingping
 */
public class EventBusExample {

    public static void main(String[] args) {
        EventBusExample eventBusExamples = new EventBusExample();
        Capz capz = new CapzImpl();
        EventBus eventBus = capz.eventBus();
        eventBusExamples.example8(eventBus);
        eventBusExamples.example9(eventBus);
    }


    public void example1(EventBus eventBus) {

        MessageConsumer<String> consumer1 = eventBus.consumer("news.uk.sport", message -> {
            System.out.println("consumer-1: I have received a message: " + message.body());
        });
        // TODO ???
        consumer1.completionHandler(res -> {
            if (res.succeeded()) {
                System.out.println("The handler registration has reached all nodes");
            } else {
                System.out.println("Registration failed!");
            }
        });


        MessageConsumer<String> consumer2 = eventBus.consumer("news.uk.sport", message -> {
            System.out.println("consumer-2: I have received a message: " + message.body());
        });
    }

    public void example5(EventBus eventBus) {
        eventBus.publish("news.uk.sport", "Yay! Someone kicked a ball");
    }

    public void example6(EventBus eventBus) {
        eventBus.send("news.uk.sport", "Yay! Someone kicked a ball");
    }

    public void example8(EventBus eventBus) {
        MessageConsumer<String> consumer = eventBus.consumer("news.uk.sport");
        consumer.handler(message -> {
            System.out.println("I have received a message: " + message.body());
            message.reply("how interesting!");
        });
    }

    public void example9(EventBus eventBus) {
        eventBus.send("news.uk.sport", "Yay! Someone kicked a ball across a patch of grass", ar -> {
            if (ar.succeeded()) {
                System.out.println("Received reply: " + ar.result().body());
            }
        });
    }

}
