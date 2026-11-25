// REFERINTA - cod existent (cloud, common-api), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
// --- com.bnp.pf.transformation.commons.api.generalproxyobs.entity ---
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "APPLICATION_REQUEST")
public class ApplicationRequestEntity {

    @Id
    @Column(name = "BUSINESS_KEY")
    private Long businessKey;

    @Column(name = "APPLICATION_JSON", length = 100000)
    private String applicationJson;
    @Column(name = "STATUS")
    private String status;
    @Column(name = "CREATED_BY")
    private String createdBy;
    @Column(name = "CREATED_DATE")
    private LocalDate createdDate;
    @Column(name = "MODIFIED_BY")
    private String modifiedBy;
    @Column(name = "MODIFIED_DATE")
    private LocalDate modifiedDate;
}

// --- com.bnp.pf.transformation.commons.api.generalproxyobs.dto ---
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationRequestDto {
    private Long businessKey;
    private String applicationJson;
    private String status;
    private LocalDate createdDate;
    private LocalDate modifiedDate;
    private String createdBy;
    private String modifiedBy;
}

// --- com.bnp.pf.transformation.commons.api.generalproxyobs.repo ---
@Repository
public interface ApplicationRequestRepository extends JpaRepository<ApplicationRequestEntity, Long> {

    Optional<ApplicationRequestEntity> findByBusinessKey(Long businessKey);

    @Transactional
    @Modifying
    @Query("update ApplicationRequestEntity app set app.status = :status where app.businessKey = :businessKey")
    void setApplicationRequestByStatus(String status, Long businessKey);
}
