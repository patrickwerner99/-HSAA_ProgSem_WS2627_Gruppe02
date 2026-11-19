package de.hsaa.shop.account;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Gespeicherte Adressen. Gesucht wird immer zusammen mit dem Besitzer, damit niemand fremde Adressen sieht. */
public interface SavedAddressRepository extends JpaRepository<SavedAddress, Long> {

	List<SavedAddress> findByOwnerIdOrderByStandardDescCreatedAtAsc(Long ownerId);

	Optional<SavedAddress> findByIdAndOwnerId(Long id, Long ownerId);

	long countByOwnerId(Long ownerId);
}
