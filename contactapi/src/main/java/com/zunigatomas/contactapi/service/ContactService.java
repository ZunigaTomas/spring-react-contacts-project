package com.zunigatomas.contactapi.service;

import com.zunigatomas.contactapi.domain.Contact;
import com.zunigatomas.contactapi.repository.ContactRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static com.zunigatomas.contactapi.constants.Constant.PHOTO_DIRECTORY;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class ContactService {

    public final ContactRepository repository;

    public Page<Contact> getAllContacts(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("name")));
    }
    public Contact getContact(String id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Contact not found."));
    }
    public Contact createContact(Contact contact) {
        return repository.save(contact);
    }
    public void deleteContact(Contact contact) {
        Contact deletedContact = repository.findById(contact.getId()).orElseThrow(() -> new RuntimeException("Contact not found."));
        repository.delete(deletedContact);
    }

    public String uploadPhoto(String id, MultipartFile file) {
        log.info("Saving picture for user id: {}", id);
        Contact contact = getContact(id);
        String photoUrl = photoFunction.apply(id, file);
        contact.setPhotoUrl(photoUrl);
        repository.save(contact);
        return photoUrl;
    }

    private final UnaryOperator<String> fileExtension = filename -> Optional.of(filename).filter(name -> name.contains("."))
            .map(name -> name.substring(filename.lastIndexOf("."))).orElse(".png");

    private final BiFunction<String, MultipartFile, String> photoFunction = (id, image) -> {
        String filename = id + fileExtension.apply(image.getOriginalFilename());
        try {
            Path fileStorageLocation = Paths.get(PHOTO_DIRECTORY).toAbsolutePath().normalize();
            if(!Files.exists(fileStorageLocation)) {
                Files.createDirectories(fileStorageLocation);
            }
            Files.copy(image.getInputStream(), fileStorageLocation.resolve(filename), REPLACE_EXISTING);
            return ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/contacts/image/" + filename)
                    .toUriString();
        } catch (Exception exception) {
            throw new RuntimeException("Unable to save image.");
        }
    };
}