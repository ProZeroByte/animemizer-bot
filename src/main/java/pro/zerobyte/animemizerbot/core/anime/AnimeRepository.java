package pro.zerobyte.animemizerbot.core.anime;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnimeRepository extends ListCrudRepository<Anime, Long> {

    Optional<Anime> findByPublicIdIgnoreCase(String publicId);

    Optional<Anime> findByNameIgnoreCase(String name);

    Optional<Anime> findByRussianNameIgnoreCase(String name);

    Optional<Anime> findByEnglishNameIgnoreCase(String name);

    Optional<Anime> findByPublicId(String publicId);
}
