package com.vtcorp.store.services;

import com.vtcorp.store.dtos.ProductRequestDTO;
import com.vtcorp.store.entities.*;
import com.vtcorp.store.mappers.ProductMapper;
import com.vtcorp.store.repositories.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final String UPLOAD_DIR = "src/main/resources/static/images/products";
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;

    @Autowired
    public ProductService(ProductRepository productRepository, ProductMapper productMapper, BrandRepository brandRepository, CategoryRepository categoryRepository, ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
    }

    public List<Product> getActiveProducts() {
        return productRepository.findByActive(true);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Transactional
    public Product addProduct(ProductRequestDTO productRequestDTO) {

        Brand brand = brandRepository.findById(productRequestDTO.getBrandId())
                .orElseThrow(() -> new RuntimeException("Brand not found"));
        List<Category> categories = categoryRepository.findAllById(productRequestDTO.getCategoryIds());
        if (categories.size() != productRequestDTO.getCategoryIds().size()) {
            throw new RuntimeException("One or more categories not found");
        }

        Product product = productMapper.toEntity(productRequestDTO);
        product.setBrand(brand);
        product.setCategories(categories);
        List<ProductImage> images = handleProductImages(productRequestDTO.getNewImageFiles(), product);
        product.setProductImages(images);
        try {
            productRepository.save(product);
            return product;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save product", e);
        }
    }

    @Transactional
    public Product updateProduct(ProductRequestDTO productRequestDTO) {
        Product product = productRepository.findById(productRequestDTO.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        Brand brand = brandRepository.findById(productRequestDTO.getBrandId())
                .orElseThrow(() -> new RuntimeException("Brand not found"));
        List<Category> categories = categoryRepository.findAllById(productRequestDTO.getCategoryIds());
        if (categories.size() != productRequestDTO.getCategoryIds().size()) {
            throw new RuntimeException("One or more categories not found");
        }
        List<ProductImage> newImages = handleProductImages(productRequestDTO.getNewImageFiles(), product);
        List<ProductImage> savedImages = product.getProductImages();
        List<ProductImage> imagesToDelete = null;
        if (savedImages != null && !savedImages.isEmpty()) {
            imagesToDelete = new ArrayList<>(savedImages);
            List<Long> imageToKeepIds = productRequestDTO.getImageIds();
            if (imageToKeepIds != null && !imageToKeepIds.isEmpty()) {
                List<ProductImage> imagesToKeep = productImageRepository.findAllById(imageToKeepIds);
                imagesToDelete.removeAll(imagesToKeep);
                newImages.addAll(imagesToKeep);
            }
        }

        // annotation @Transactional is used to roll back the transaction if an exception occurs
        // any changes made to managed entities will be automatically saved to the database
        // no need to call save() method
        productMapper.updateEntity(productRequestDTO, product);
        product.setBrand(brand);
        product.setCategories(categories);
        product.getProductImages().clear();
        product.getProductImages().addAll(newImages);

        if (imagesToDelete != null) {
            removeImages(imagesToDelete);
        }
        return product;
    }

    private List<ProductImage> handleProductImages(List<MultipartFile> imageFiles, Product product) {
        List<ProductImage> productImageList = new ArrayList<>();
        if (imageFiles != null) {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            try {
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                for (MultipartFile image : imageFiles) {
                    String storedFileName = (new Date()).getTime() + "_" + image.getOriginalFilename();
                    try (InputStream inputStream = image.getInputStream()) {
                        Files.copy(inputStream, Paths.get(UPLOAD_DIR, storedFileName), StandardCopyOption.REPLACE_EXISTING);
                        productImageList.add(ProductImage.builder()
                                .imagePath(storedFileName)
                                .product(product)
                                .build());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to save image", e);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to create upload directory", e);
            }
        }
        return productImageList;
    }

    private void removeImages(List<ProductImage> images) {
        for (ProductImage image : images) {
            Path imagePath = Paths.get(UPLOAD_DIR, image.getImagePath());
            try {
                Files.deleteIfExists(imagePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete image", e);
            }
        }
    }

    public String deactivateProduct(long id) {
        productRepository.setActivateProduct(false, id);
        return "Product deactivated";
    }

    public String activateProduct(long id) {
        productRepository.setActivateProduct(true, id);
        return "Product activated";
    }

    public List<Product> searchProducts(String keyword, List<Long> categoryIds, Long brandId) {
        return productRepository.findAll().stream()
                .filter(product -> (keyword == null || product.getName().contains(keyword) || product.getDescription().contains(keyword)) &&
                        (categoryIds == null || product.getCategories().stream().anyMatch(category -> categoryIds.contains(category.getCategoryId()))) &&
                        (brandId == null || product.getBrand().getBrandId() == brandId))
                .collect(Collectors.toList());
    }



}