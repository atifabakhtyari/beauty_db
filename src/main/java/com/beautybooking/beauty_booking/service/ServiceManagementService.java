
package com.beautybooking.beauty_booking.service;

import com.beautybooking.beauty_booking.dto.CreateServiceRequest;
import com.beautybooking.beauty_booking.dto.CreateServiceCategoryRequest;
import com.beautybooking.beauty_booking.dto.UpdateServiceRequest;
import com.beautybooking.beauty_booking.dto.ServiceResponse;
import com.beautybooking.beauty_booking.dto.ServiceCategoryResponse;
import com.beautybooking.beauty_booking.dto.ServiceCategoryListResponse;

import com.beautybooking.beauty_booking.entity.User;
import com.beautybooking.beauty_booking.entity.Provider;
import com.beautybooking.beauty_booking.entity.ProviderApprovalStatus;
import com.beautybooking.beauty_booking.entity.ProviderService;
import com.beautybooking.beauty_booking.entity.ServiceCategory;
import com.beautybooking.beauty_booking.entity.ServicePortfolioItem;

import com.beautybooking.beauty_booking.repository.UserRepository;
import com.beautybooking.beauty_booking.repository.ProviderRepository;
import com.beautybooking.beauty_booking.repository.ProviderServiceRepository;
import com.beautybooking.beauty_booking.repository.ServiceCategoryRepository;
import com.beautybooking.beauty_booking.repository.ServicePortfolioItemRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class ServiceManagementService {

    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final ProviderServiceRepository serviceRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final ServicePortfolioItemRepository portfolioRepository;
    private final ProviderImageStorageService storage;

    public ServiceManagementService(
            UserRepository userRepository,
            ProviderRepository providerRepository,
            ProviderServiceRepository serviceRepository,
            ServiceCategoryRepository categoryRepository,
            ServicePortfolioItemRepository portfolioRepository,
            ProviderImageStorageService storage) {

        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.serviceRepository = serviceRepository;
        this.categoryRepository = categoryRepository;
        this.portfolioRepository = portfolioRepository;
        this.storage = storage;
    }

    // دریافت ارائه‌دهنده فعلی
    private Provider getCurrentProvider() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getName() == null ||
                authentication.getName().equals("anonymousUser")) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "لطفاً ابتدا وارد حساب کاربری شوید"
            );
        }

        String email = authentication.getName();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "کاربر پیدا نشد"
                ));

        Provider provider = providerRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "پروفایل ارائه‌دهنده پیدا نشد"
                ));

        if (!provider.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "حساب ارائه‌دهنده غیرفعال است"
            );
        }

        return provider;
    }

    // بررسی واقعی بودن دسته‌بندی
    private ServiceCategory getCategory(String categoryId) {

        if (categoryId == null || categoryId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "انتخاب دسته‌بندی الزامی است"
            );
        }

        return categoryRepository.findById(categoryId.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "دسته‌بندی انتخاب‌شده وجود ندارد"
                ));
    }

    // قیمت در دیتابیس BIGINT است؛ بنابراین فقط عدد صحیح مجاز است.
    // این بررسی از گرد شدن خاموش مقادیر اعشاری جلوگیری می‌کند.
    private void validatePrice(BigDecimal price) {

        if (price == null) {
            return;
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "قیمت باید بیشتر از صفر باشد"
            );
        }

        if (price.remainder(BigDecimal.ONE)
                .compareTo(BigDecimal.ZERO) != 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "قیمت باید یک عدد صحیح باشد"
            );
        }
    }

    // تبدیل Entity به DTO
    private ServiceResponse toResponse(ProviderService service) {

        ServiceCategory category = service.getCategory();

        return new ServiceResponse(
                service.getServiceId(),
                service.getServiceName(),
                service.getPrice(),
                service.getDurationMinutes(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                service.isActive()
        );
    }

    // 1. فهرست خدمات و جست‌وجو
    @Transactional(readOnly = true)
    public List<ServiceResponse> getServices(String keyword) {

        Provider provider = getCurrentProvider();

        List<ProviderService> services;

        if (keyword == null || keyword.isBlank()) {
            services = serviceRepository.findAllByProviderId(
                    provider.getProviderId()
            );
        } else {
            services = serviceRepository
                    .findByProviderIdAndServiceNameContainingIgnoreCase(
                            provider.getProviderId(),
                            keyword.trim()
                    );
        }

        return services.stream()
                .map(this::toResponse)
                .toList();
    }

    // 2. دریافت جزئیات خدمت
    @Transactional(readOnly = true)
    public ServiceResponse getService(Long serviceId) {

        Provider provider = getCurrentProvider();

        ProviderService service = serviceRepository
                .findByServiceIdAndProviderId(
                        serviceId,
                        provider.getProviderId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "خدمت موردنظر پیدا نشد"
                ));

        return toResponse(service);
    }

    // 3. ایجاد خدمت
    @Transactional
    public ServiceResponse createService(CreateServiceRequest request) {

        Provider provider = getCurrentProvider();

        ServiceCategory category = getCategory(request.categoryId());

        validatePrice(request.price());

        boolean duplicate = serviceRepository
                .existsByProviderIdAndServiceNameIgnoreCase(
                        provider.getProviderId(),
                        request.serviceName().trim()
                );

        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "این خدمت قبلاً توسط شما ثبت شده است"
            );
        }

        ProviderService service = new ProviderService();

        service.setProviderId(provider.getProviderId());
        service.setServiceName(request.serviceName().trim());
        service.setPrice(request.price());
        service.setDurationMinutes(request.durationMinutes());
        service.setCategory(category);

        // سیاست انتشار: فقط ارائه‌دهنده تأییدشده می‌تواند خدمت را منتشر کند.
        // خدمات ارائه‌دهندگان در انتظار تأیید یا ردشده به صورت پیش‌نویس ذخیره می‌شوند.
        service.setActive(
                provider.getApprovalStatus()
                        == ProviderApprovalStatus.APPROVED
        );

        ProviderService savedService;

        try {
            // flush باعث می‌شود نقض محدودیت یکتایی همان‌جا تشخیص داده شود.
            savedService = serviceRepository.saveAndFlush(service);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "این خدمت قبلاً توسط شما ثبت شده است"
            );
        }

        return toResponse(savedService);
    }

    // 4. ویرایش خدمت
    @Transactional
    public ServiceResponse updateService(
            Long serviceId,
            UpdateServiceRequest request) {

        Provider provider = getCurrentProvider();

        ProviderService service = serviceRepository
                .findByServiceIdAndProviderId(
                        serviceId,
                        provider.getProviderId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "خدمت موردنظر پیدا نشد"
                ));

        if (request.serviceName() == null &&
                request.price() == null &&
                request.durationMinutes() == null &&
                request.categoryId() == null &&
                request.isActive() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "حداقل یک فیلد برای ویرایش ارسال کنید"
            );
        }

        // ویرایش نام خدمت
        if (request.serviceName() != null) {

            String newName = request.serviceName().trim();

            if (newName.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "نام خدمت نمی‌تواند خالی باشد"
                );
            }

            boolean duplicate = serviceRepository
                    .existsByProviderIdAndServiceNameIgnoreCaseAndServiceIdNot(
                            provider.getProviderId(),
                            newName,
                            serviceId
                    );

            if (duplicate) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "خدمت دیگری با این نام وجود دارد"
                );
            }

            service.setServiceName(newName);
        }

        // ویرایش قیمت
        if (request.price() != null) {

            validatePrice(request.price());

            service.setPrice(request.price());
        }

        // ویرایش مدت زمان
        if (request.durationMinutes() != null) {

            if (request.durationMinutes() < 5 ||
                    request.durationMinutes() > 600) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "مدت زمان باید بین 5 تا 600 دقیقه باشد"
                );
            }

            service.setDurationMinutes(request.durationMinutes());
        }

        // ویرایش دسته‌بندی
        if (request.categoryId() != null) {

            ServiceCategory category =
                    getCategory(request.categoryId());

            service.setCategory(category);
        }

        // تغییر وضعیت انتشار
        if (request.isActive() != null) {

            boolean publish = request.isActive();

            if (publish &&
                    provider.getApprovalStatus()
                            != ProviderApprovalStatus.APPROVED) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "فقط ارائه‌دهنده تأییدشده می‌تواند خدمت را منتشر کند"
                );
            }

            service.setActive(publish);
        }

        try {
            return toResponse(serviceRepository.saveAndFlush(service));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "خدمت دیگری با این نام وجود دارد"
            );
        }
    }

    // 5. حذف خدمت
    @Transactional
    public void deleteService(Long serviceId) {

        Provider provider = getCurrentProvider();

        ProviderService service = serviceRepository
                .findByServiceIdAndProviderId(
                        serviceId,
                        provider.getProviderId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "خدمت موردنظر پیدا نشد"
                ));

        // فایل‌های نمونه‌کار پس از commit حذف می‌شوند تا در صورت rollback
        // داده‌ها و فایل‌ها ناهماهنگ نشوند.
        List<String> imageUrls = portfolioRepository
                .findByServiceIdOrderByCreatedAtDesc(serviceId)
                .stream()
                .map(ServicePortfolioItem::getImageUrl)
                .toList();

        serviceRepository.delete(service);
        serviceRepository.flush();

        deleteFilesAfterCommit(imageUrls);
    }

    private void deleteFilesAfterCommit(List<String> imageUrls) {

        if (imageUrls.isEmpty()) {
            return;
        }

        Runnable cleanup = () -> imageUrls.forEach(storage::delete);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            cleanup.run();
                        }
                    }
            );

            return;
        }

        cleanup.run();
    }

    // 6. دریافت فهرست دسته‌بندی‌های خدمات
    @Transactional(readOnly = true)
    public ServiceCategoryListResponse getServiceCategories() {

        getCurrentProvider();

        List<ServiceCategoryResponse> items =
                categoryRepository.findTop200ByOrderByNameAsc()
                        .stream()
                        .map(category -> new ServiceCategoryResponse(
                                category.getId(),
                                category.getName()
                        ))
                        .toList();

        return new ServiceCategoryListResponse(items);
    }

    // 7. ایجاد دسته‌بندی خدمات
    // دسته‌بندی‌ها داده مرجع سراسری هستند و فقط مدیر می‌تواند آن‌ها را بسازد؛
    // این محدودیت در لایه کنترلر و Spring Security اعمال می‌شود.
    @Transactional
    public ServiceCategoryResponse createServiceCategory(
            CreateServiceCategoryRequest request) {

        String id = request.id().trim();
        String name = request.name().trim();

        if (categoryRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "دسته‌بندی با این شناسه قبلاً ثبت شده است"
            );
        }

        if (categoryRepository.countByNormalizedName(
                name.toLowerCase(Locale.ROOT)) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "دسته‌بندی با این نام قبلاً ثبت شده است"
            );
        }

        ServiceCategory category = new ServiceCategory(id, name);

        try {
            // flush باعث می‌شود نقض ایندکس یکتای دیتابیس همان‌جا تشخیص داده شود.
            categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "دسته‌بندی با این شناسه یا نام قبلاً ثبت شده است"
            );
        }

        return new ServiceCategoryResponse(
                category.getId(),
                category.getName()
        );
    }
}
