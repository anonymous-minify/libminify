class ExceptionHelper {
    Object checkIfNotNull(Object obj) {
        try {
            if (obj == null) {
                throw new RuntimeException("obj should not be null");
            }
            return obj;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
