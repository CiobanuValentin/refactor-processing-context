// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.exception;

@Getter
public class ProcessingFailedException extends RuntimeException {
    private final transient ProcessingContext processingContext;

    public ProcessingFailedException(ProcessingContext processingContext, String message, Throwable cause) {
        super(message, cause);
        this.processingContext = processingContext;
    }

    public static Optional<ProcessingContext> findIn(Throwable throwable) {
        for (Throwable c = throwable; c != null; c = c.getCause()) {
            if (c instanceof ProcessingFailedException processingFailedException) {
                return Optional.ofNullable(processingFailedException.processingContext);
            }
        }
        return Optional.empty();
    }

}
