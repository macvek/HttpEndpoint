package pl.almatron.httpendpoint;

/**
 * HttpEndpoint
 *
 * @author macvek
 */
public class CompareByteArrays {

    private final byte[] pattern;

    public CompareByteArrays(byte[] pattern) {
        this.pattern = pattern;
    }

    /**
     * Calculate offset of expectedBytes from pattern. If it matches, then it return 0
     * If it is complitely different array, then it return argument's length
     * If it is only partially matching, like:
     * PATTERN:  A A A A
     * EXPECTED: 0 1 A A 
     * then it return 2 as it is first index of matching pattern in given array
     * @param expectedBytes
     * @return 
     */
    public int findOffset(byte[] expectedBytes) {
        int offset = 0;
        offsetLoop:
        for (; offset < expectedBytes.length; offset++) {
            for (int seeker = 0; seeker < pattern.length - offset; seeker++) {
                if (expectedBytes[offset + seeker] != pattern[seeker]) {
                    continue offsetLoop;
                }
            }

            break;
        }

        return offset;
    }
}
