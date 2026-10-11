package Group04.customer_service.controller;

import Group04.customer_service.model.Customer;
import Group04.customer_service.repository.CustomerRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<?> createCustomer(@Valid @RequestBody Customer customer) {
        String email = customer.getEmail().trim().toLowerCase();
        if (customerRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "A customer with email " + email + " already exists"));
        }

        customer.setId(null);
        customer.setEmail(email);
        customer.setActive(true);
        LocalDateTime now = LocalDateTime.now();
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        return new ResponseEntity<>(customerRepository.save(customer), HttpStatus.CREATED);
    }

    // READ ALL (optional filter: /api/customers?email=a@b.com)
    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers(@RequestParam(required = false) String email) {
        if (email != null && !email.isBlank()) {
            Optional<Customer> found = customerRepository.findByEmail(email.trim().toLowerCase());
            return ResponseEntity.ok(found.map(List::of).orElse(List.of()));
        }
        return ResponseEntity.ok(customerRepository.findAll());
    }

    // READ ONE
    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerById(@PathVariable String id) {
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isEmpty()) {
            return notFound(id);
        }
        return ResponseEntity.ok(customer.get());
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable String id, @Valid @RequestBody Customer updated) {
        Optional<Customer> found = customerRepository.findById(id);
        if (found.isEmpty()) {
            return notFound(id);
        }

        Customer existing = found.get();
        String email = updated.getEmail().trim().toLowerCase();

        Optional<Customer> sameEmail = customerRepository.findByEmail(email);
        if (sameEmail.isPresent() && !sameEmail.get().getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Another customer already uses email " + email));
        }

        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setEmail(email);
        existing.setPhone(updated.getPhone());
        existing.setAddress(updated.getAddress());
        existing.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(customerRepository.save(existing));
    }

    // DEACTIVATE
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateCustomer(@PathVariable String id) {
        return changeActive(id, false);
    }

    // ACTIVATE
    @PatchMapping("/{id}/activate")
    public ResponseEntity<?> activateCustomer(@PathVariable String id) {
        return changeActive(id, true);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable String id) {
        if (!customerRepository.existsById(id)) {
            return notFound(id);
        }
        customerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> changeActive(String id, boolean active) {
        Optional<Customer> found = customerRepository.findById(id);
        if (found.isEmpty()) {
            return notFound(id);
        }

        Customer existing = found.get();
        existing.setActive(active);
        existing.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(customerRepository.save(existing));
    }

    private ResponseEntity<?> notFound(String id) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Customer not found with id: " + id));
    }
}