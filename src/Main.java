import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static Logger LOGGER = LogManager.getLogger(Main.class);

    public static void main(String[] args) {

        LOGGER.info("Main function initialized");

        new engine.Windows().run();

        LOGGER.info("Main function terminated");

    }
}
