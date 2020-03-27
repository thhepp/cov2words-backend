package io.hepp.cov2words.common.constant;

/**
 * Class that contains the API paths.
 *
 * @author Thomas Hepp, thomas@hepp.io
 */
public final class Paths {
    public static final String BASE_PATH = "/api";

    /**
     * Private constructor to avoid accidental creation of an instance.
     */
    private Paths() {
    }

    /**
     * Contains the paths.
     */
    public static final class PairPaths {
        private static final String BASE_PATH = "/pair";
        public static final String CREATE_PAIR = BASE_PATH + "/create";
        public static final String GET_ANSWER = BASE_PATH + "/answers/get";
        public static final String GET_LANGUAGES = BASE_PATH + "/languages/get";
        public static final String GET_WORDS = BASE_PATH + "/get";

        /**
         * Private constructor to avoid accidental creation of an instance.
         */
        private PairPaths() {
        }
    }
}