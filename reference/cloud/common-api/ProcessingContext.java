// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.generalproxyobs.dto;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingContext extends ProcessingContextBase {
    //from CollectError
    private Map<String, Signal> errorSignalMap = new HashMap<>();
    private Signal statusMessage = new Signal();

    //from GeneralNullOrEmptyCheckService
    private Signal errorMessage;
    private Signal signal;

}
