package chatper1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Art of Multiprocessor Programming, Chapter 1 Exercise 8.
 *
 * Preconditions
 *
 * 1. 5 philosophers surround a table
 * 2. 5 chopsticks in between each philosopher
 * 3. each philosopher thinks for a while and then sits down to eat
 * 4. each philosopher needs the chopsticks on each side of them in order to eat.
 * 5. Each chopstick is shared between the philosopher to it’s left, and to it’s right.
 *
 * Requirements
 *
 * 1. Two philosophers cannot hold the same chopstick at the same time.
 * 2. chatper1.Philosophers cannot starve
 * 	1. They must _eventually_ obtain both chopsticks needed
 * 3. chatper1.Philosophers cannot deadlock
 */

// Shallow extension for additional debugging.
class Chopstick extends ReentrantLock {
    private final String name;

    public Chopstick(final String name) {
        super();

        this.name = name;
    }

    @Override
    public void lock() {
        super.lock();

        System.out.println(String.format("%s locked by thread %s", this.name, this.getOwner().getName()));
    }

    @Override
    public void unlock() {
        super.unlock();

        System.out.println(String.format("%s unlocked by thread %s", this.name, this.getOwner().getName()));
    }
}

class Philosopher extends Thread {
    private final Random random = new Random();
    private final Chopstick leftChopstick;
    private final Chopstick rightChopstick;
    private int timesEaten = 0;

    public Philosopher(Chopstick rightChopstick, String name, Chopstick leftChopstick) {
        super();
        this.setName(name);
        this.rightChopstick = rightChopstick;
        this.leftChopstick = leftChopstick;
    }

    public void run() {
        while (true) {
            final int timeToThink = random.nextInt(10000) + 1000;

            try {
                // "Think" for a while.
                say(String.format("I'm thinking for %d milliseconds", timeToThink));
                sleep(timeToThink);
                say("I'm done thinking, time to eat!");

                // Try to acquire both chopsticks.
                this.leftChopstick.lock();
                this.rightChopstick.lock();

                say("I have both chopsticks, eating now");
                this.timesEaten++;
            } catch (InterruptedException e) {
                System.out.println("failed to think: " + e.getMessage());
            } finally { // make sure to release both chopsticks before thinking.
                this.leftChopstick.unlock();
                this.rightChopstick.unlock();
            }
        }
    }

    public int getTimesEaten() {
        return this.timesEaten;
    }

    private void say(final String message) {
//        System.out.println(String.format("%s says '%s'", this.getName(), message));
    }
}

public class Philosophers {
    public static void main(String[] args) {
        Chopstick c1 = new Chopstick("c1");
        Chopstick c2 = new Chopstick("c2");
        Chopstick c3 = new Chopstick("c3");
        Chopstick c4 = new Chopstick("c4");
        Chopstick c5 = new Chopstick("c5");

        // Philosophers arranged around a "circular" table.
        // Each Philosopher shares a chopstick with their neighbor.
        List<Philosopher> philosophers = new ArrayList<>() {{
            add(new Philosopher(c5, "p1", c1));
            add(new Philosopher(c1, "p2", c2));
            add(new Philosopher(c2, "p3", c3));
            add(new Philosopher(c3, "p4", c4));
            add(new Philosopher(c4, "p5", c5));
        }};

        philosophers.forEach(Thread::start);

        // Print the number of times each philosopher has eaten, every second.
        ScheduledExecutorService reportExecutor = Executors.newSingleThreadScheduledExecutor();
        Runnable reportRunnable = () -> {
            StringBuilder sb = new StringBuilder("# of times each philosopher has eaten\n");

            synchronized (philosophers) {
                philosophers.forEach(philosopher -> sb.append(String.format("%s = %d\n", philosopher.getName(),
                        philosopher.getTimesEaten())));
            }

            System.out.println(sb.toString());
        };

        reportExecutor.scheduleAtFixedRate(reportRunnable, 0L, 1, TimeUnit.SECONDS);
    }
}
