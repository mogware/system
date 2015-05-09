package org.mogware.system.threading;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.mogware.system.OperationCanceledException;

public class ProducerConsumerQueueFactory {
    public static <T> ProducerConsumerQueue<T> linkedQueue(int capasity) {
        return new Queue(new LinkedBlockingQueue(capasity));
    }

    public static <T> ProducerConsumerQueue<T> arrayQueue(int capasity) {
        return new Queue(new ArrayBlockingQueue(capasity));
    }

    public static <T> ProducerConsumerQueue<T> priorityQueue(int initialSize) {
        return new Queue(new PriorityBlockingQueue(initialSize));
    }
    
    public static <T> ProducerConsumerQueue<T> synchronousQueue() { 
        return new Queue(new SynchronousQueue());
    }        
    
    static final class Queue<T> implements ProducerConsumerQueue<T> {
        private final BlockingQueue<T> queue;

        private final AtomicInteger currentAdders = new AtomicInteger();
        private final static int COMPLETE_ADDING_ON_MASK = (int) 0x80000000;

        private final CancellationTokenSource consumersCancellationSource;
        private final CancellationTokenSource producersCancellationSource;

        public Queue(BlockingQueue<T> queue) {
            this.queue = queue;
            this.currentAdders.set(0);
            this.consumersCancellationSource = new CancellationTokenSource();
            this.producersCancellationSource = new CancellationTokenSource();
        }

        @Override
        public void enqueue(T item) {
            this.enqueue(item, CancellationTokenSource.getTokenNone());
        }   
        
        @Override
        public void enqueue(T item, CancellationToken ct) {
            if (ct.isCancellationRequested())
                throw new OperationCanceledException();
            if (this.isAddingCompleted())
                throw new IllegalStateException();
            SpinWait spinner = new SpinWait();
            while (true) {
                int observedAdders = this.currentAdders.get();
                if ((observedAdders & COMPLETE_ADDING_ON_MASK) != 0) {
                    spinner.reset();
                    while (this.currentAdders.get() != COMPLETE_ADDING_ON_MASK)
                        spinner.spinOnce();
                    throw new IllegalStateException();
                }
                if (this.currentAdders.compareAndSet(observedAdders,
                        observedAdders + 1))
                    break;
                spinner.spinOnce();
            }
            CancellationTokenSource linkedTokenSource =
                    CancellationTokenSource.createLinkedTokenSource(ct,
                            this.producersCancellationSource.getToken());
            linkedTokenSource.getToken().registerInterrupt();
            try {
                if (ct.isCancellationRequested())
                    throw new OperationCanceledException();
                this.queue.put(item);
            } catch (InterruptedException ex) {
                if (ct.isCancellationRequested())
                    throw new OperationCanceledException();
                throw new IllegalStateException();
            }
            finally {
                linkedTokenSource.dispose();
                this.currentAdders.decrementAndGet();
            }
        }

        @Override
        public T dequeue() {
            return dequeue(CancellationTokenSource.getTokenNone());
        }   
        
        @Override
        public T dequeue(CancellationToken ct) {
            if (ct.isCancellationRequested())
                throw new OperationCanceledException();
            if (this.isCompleted())
                throw new IllegalStateException();
            CancellationTokenSource linkedTokenSource =
                    CancellationTokenSource.createLinkedTokenSource(ct,
                            this.consumersCancellationSource.getToken());
            linkedTokenSource.getToken().registerInterrupt();
            try {
                return this.queue.take();
            } catch (InterruptedException ex) {
                if (ct.isCancellationRequested())
                    throw new OperationCanceledException();
                throw new IllegalStateException();
            }
            finally {
                linkedTokenSource.dispose();
                if (this.isCompleted())
                    this.cancelWaitingConsumers();
            }
        }

        @Override
        public void CompleteAdding() {
            if (this.isAddingCompleted())
                return;
            SpinWait spinner = new SpinWait();
            while (true) {
                int observedAdders = this.currentAdders.get();
                if ((observedAdders & COMPLETE_ADDING_ON_MASK) != 0) {
                    spinner.reset();
                    while (this.currentAdders.get() != COMPLETE_ADDING_ON_MASK)
                        spinner.spinOnce();
                    return;
                }
                if (this.currentAdders.compareAndSet(observedAdders,
                        observedAdders | COMPLETE_ADDING_ON_MASK)) {
                    spinner.reset();
                    while (this.currentAdders.get() != COMPLETE_ADDING_ON_MASK)
                        spinner.spinOnce();
                    if (this.queue.isEmpty())
                        cancelWaitingConsumers();
                    cancelWaitingProducers();
                    return;
                }
                spinner.spinOnce();
            }
        }

        @Override
        public boolean isAddingCompleted() {
            return this.currentAdders.get() == COMPLETE_ADDING_ON_MASK;
        }

        @Override
        public boolean isCompleted() {
            return this.isAddingCompleted() && this.queue.isEmpty();
        }

        private void cancelWaitingConsumers() {
            this.consumersCancellationSource.cancel();
        }

        private void cancelWaitingProducers() {
            this.producersCancellationSource.cancel();
        }
    }
}
