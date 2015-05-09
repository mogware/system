package org.mogware.system.threading;

/**
* Provides capabilities for thread-safe Producer/Consumer Queue.
*/

public interface ProducerConsumerQueue<T> {
    /**
    * Adds an item to the queue
    */
    void enqueue(T item);
    
    /**
    * Adds an item to the queue with cancellation option
    */
    void enqueue(T item, CancellationToken ct);

    /**
    * Takes an item from the queue
    */
    T dequeue();

    /**
    * Takes an item from the queue with cancellation option
    */
    T dequeue(CancellationToken ct);
    
    /**
    * Marks the queue as not accepting any more additions.
    */
    void CompleteAdding();

    /**
    * Gets whether queue has been marked as complete for adding.
    */
     boolean isAddingCompleted();

     /**
    * Gets whether queue has been marked as complete for adding and is empty.
    */
    boolean isCompleted();
}
