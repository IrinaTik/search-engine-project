package searchengine.services.actions;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GenerateLockAction {
    public static final ReentrantReadWriteLock PAGE_PARSE_LOCK = new ReentrantReadWriteLock();
    public static final ReentrantReadWriteLock SITE_PARSE_LOCK = new ReentrantReadWriteLock();


    public static void lockPageParseWriteLock() {
        PAGE_PARSE_LOCK.writeLock().lock();
    }

    public static void unlockPageParseWriteLock() {
        PAGE_PARSE_LOCK.writeLock().unlock();
        try {
            while (PAGE_PARSE_LOCK.isWriteLockedByCurrentThread()) {
                Thread.sleep((long) (Math.random() * 1000));
            }
        } catch (InterruptedException e) {
            // ignore exception
        }
    }

    public static void lockPageParseReadLock() {
        PAGE_PARSE_LOCK.readLock().lock();
    }

    public static void unlockPageParseReadLock() {
        PAGE_PARSE_LOCK.readLock().unlock();
    }

    public static void lockSiteParseWriteLock() {
        SITE_PARSE_LOCK.writeLock().lock();
    }

    public static void unlockSiteParseWriteLock() {
        SITE_PARSE_LOCK.writeLock().unlock();
    }

    public static void lockSiteParseReadLock() {
        SITE_PARSE_LOCK.readLock().lock();
    }

    public static void unlockSiteParseReadLock() {
        SITE_PARSE_LOCK.readLock().unlock();
    }

}
