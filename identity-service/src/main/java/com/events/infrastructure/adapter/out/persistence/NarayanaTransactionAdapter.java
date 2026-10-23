package com.events.infrastructure.adapter.out.persistence;
import com.events.application.port.out.TransactionPort;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.narayana.jta.QuarkusTransaction;
import java.util.function.Supplier;
/** REMOVED: Spring TransactionTemplate; JTA boundaries stay behind the same port. */
@ApplicationScoped
public class NarayanaTransactionAdapter implements TransactionPort {
    public <T> T execute(Supplier<T> action) { return QuarkusTransaction.joiningExisting().call(action::get); }
}
