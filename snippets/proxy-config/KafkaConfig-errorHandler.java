// In KafkaConfig din fiecare proxy: errorHandler nu mai are ce semnale sa publice, fiindca ProcessingFailedException,
// ProcessingContext si KafkaErrorCollector dispar. Listenerul raspunde singur cu status KO plus error; aici ajunge
// doar o exceptie aparuta la publicare. Reincercarile raman cele de acum (interval, maxAttempts).
//
// De stiut: fiecare reincercare reia tot mesajul, deci si apelurile externe.

@Bean
public DefaultErrorHandler errorHandler() {
    return new DefaultErrorHandler((ConsumerRecord<?, ?> rec, Exception ex) ->
            log.error("Giving up on Kafka message after retries. Topic = {}, partition = {}, offset = {}",
                    rec.topic(), rec.partition(), rec.offset(), ex),
            new FixedBackOff(interval, maxAttempts));
}
