package tool.sysinfo.linux.memory;

/**
 * Define information, which is needed to get.
 *
 * @author ZHJIE
 */
public interface MemInfo {

    /**
     * Memory usage rate
     *
     * @return
     */
    double getMemUsage();

    /**
     * Memory usage rate in format: used/total - 111/1111 - unit(KB)
     *
     * @return
     */
    String getMemUsageStr();
}
