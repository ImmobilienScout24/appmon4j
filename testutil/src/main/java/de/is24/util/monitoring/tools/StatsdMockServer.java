package de.is24.util.monitoring.tools;

import de.flapdoodle.embed.process.runtime.Network;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import static java.lang.String.format;
import static java.lang.Thread.sleep;


/**
 * Mock for a statsd server using non-blocking IO according to http://www.onjava.com/pub/a/onjava/2002/09/04/nio.html?page=2
 */
public class StatsdMockServer extends ExternalResource implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(StatsdMockServer.class);

  private ServerSocketChannel server;
  private int port;
  private Selector selector;
  private Thread thread;

  @Override
  public void before() throws Throwable {
    server = ServerSocketChannel.open();
    server.configureBlocking(false);

    int maxTries = 3;
    int sleepInSec = 2;
    for (int i = 0; i < maxTries; i++) {
      try {
        port = Network.getFreeServerPort();
        server.socket().bind(new InetSocketAddress(port));
        LOG.info("StatsdMockServer started : port={}", port);
      } catch (Throwable e) {
        int lastTry = maxTries - 1;
        if (i < lastTry) {
          LOG.warn(format("Starting StatsdMockServer: try %s of %s failed. Retry after %s seconds.", i + 1, maxTries,
              sleepInSec), e);
          sleep(sleepInSec * 1000);
        } else {
          LOG.error(format("Starting StatsdMockServer: %s of %s failed. No retries left.", i + 1, maxTries));
          throw e;
        }
      }
    }

    selector = Selector.open();
    server.register(selector, SelectionKey.OP_ACCEPT);

    thread = new Thread(this);
    thread.start();
  }

  @Override
  public void after() {
    try {
      thread.interrupt();
    } catch (Exception e) {
    }
    try {
      selector.close();
    } catch (IOException e) {
    }
    try {
      server.close();
    } catch (Exception e) {
    }
  }

  public int getPort() {
    return port;
  }

  @Override
  public void run() {
    try {
      while (!thread.isInterrupted()) {
        if (selector.select() > 0) {
          Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
          while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();

            if (!key.isValid()) {
              continue;
            }

            if (key.isAcceptable()) {
              SocketChannel client = server.accept();
              client.configureBlocking(false);
              client.register(selector, SelectionKey.OP_READ);
              continue;
            }

            if (key.isReadable()) {
              SocketChannel client = (SocketChannel) key.channel();
              int BUFFER_SIZE = 32;
              ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
              try {
                client.read(buffer);
              } catch (Exception e) {
                e.printStackTrace();
              }

              continue;
            }
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }


  }
}
