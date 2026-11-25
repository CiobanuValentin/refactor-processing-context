// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.generalproxyobs.dto;

@Getter
@Setter
@ToString
@NoArgsConstructor
@SuperBuilder
public abstract class ProcessingContextBase {
    //from CollectError
    protected ErrorHandler errorHandler = new ErrorHandler();
    protected Map<CharSequence, CharSequence> errorMap = new HashMap<>();
    protected AtomicInteger index = new AtomicInteger(1);
    protected String businessKey;
    protected String flowId;
    protected Application application;
    private String applicationJson;

    //from GeneralNullOrEmptyCheckService
    private String producer;
    private Map<CharSequence, CharSequence> signalProperties;
}
