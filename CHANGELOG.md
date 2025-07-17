# [2.0.0](https://github.com/sarafrika/elimika/compare/v1.0.1...v2.0.0) (2025-07-17)


### Features

* **storage:** implement folder-based storage for profile images and course media ([005d1e8](https://github.com/sarafrika/elimika/commit/005d1e8ac811ae3dc3d9e7c2fe2d04dbe91c24d5))


### BREAKING CHANGES

* **storage:** Profile images now stored in profile_images/ folder instead of root directory

## [1.0.1](https://github.com/sarafrika/elimika/compare/v1.0.0...v1.0.1) (2025-07-17)


### Bug Fixes

* correct DECIMAL precision and scale for latitude/longitude columns ([fcebb53](https://github.com/sarafrika/elimika/commit/fcebb53a663674b69f3450782c1e38440e4dbe9f))

# 1.0.0 (2025-07-17)


* chore!: remove course definitions from database schema and seeded data scripts ([cd34c9a](https://github.com/sarafrika/elimika/commit/cd34c9a718abf5ab09c33bb420c60f1378d2e8a6))
* refactor!: remove course definitions from database schema ([68bf69a](https://github.com/sarafrika/elimika/commit/68bf69a6468049c1716989098b2bc27b803fc8e6))
* refactor!: remove course definitions from database schema ([034d677](https://github.com/sarafrika/elimika/commit/034d6773e8be2caedae2e4782cb44abf994128ab))


### Bug Fixes

* .github action workflow fixed ([6e3d60c](https://github.com/sarafrika/elimika/commit/6e3d60c45df4474bee998c3c39facf5a44396050))
* .github action workflow fixed ([4fff73c](https://github.com/sarafrika/elimika/commit/4fff73cd3db3cd4569d54566cc49150802a093f9))
* add @Query annotations for ContentTypeRepository array operations ([601c1d4](https://github.com/sarafrika/elimika/commit/601c1d47c6ebd836c3402f04d16b67b4747b6562))
* add ContentStatus converter and update API documentation ([4f275d1](https://github.com/sarafrika/elimika/commit/4f275d12df969bd297bdad1e59af2f8770d150c1))
* add licence_no field to OrganisationDTO and factory ([f29dd66](https://github.com/sarafrika/elimika/commit/f29dd6647c9bc4348541756ee6e417adf6d5737a))
* add missing status field to TrainingProgram entity and database schema ([552e237](https://github.com/sarafrika/elimika/commit/552e237c0426d96f431087466589a5a177ace053))
* Addition of User Domain ([dc21ee4](https://github.com/sarafrika/elimika/commit/dc21ee4b292e6e5e1e1ccdd8934d96411ebf37d6))
* **auth:** add Google Tink dependency for EdDSA JWT validation ([107e845](https://github.com/sarafrika/elimika/commit/107e845ad4a6c0aa6bda712b247d6aaa7906c45f))
* **auth:** add Google Tink dependency for EdDSA JWT validation ([35150b9](https://github.com/sarafrika/elimika/commit/35150b9ccd0ba13291a6dc489750af76d96bf331))
* **auth:** add Google Tink dependency for EdDSA JWT validation ([0c5b76a](https://github.com/sarafrika/elimika/commit/0c5b76afff19e03c0c7922d70da0082d6fc3c503))
* automate deployment with zero-maintenance env configuration ([f2db691](https://github.com/sarafrika/elimika/commit/f2db691cec3cbac1be0762a7efedbe6119170a09))
* automate deployment with zero-maintenance env configuration ([e77fd89](https://github.com/sarafrika/elimika/commit/e77fd89dfd87aad22eece3a819df82250a07a21c))
* automate deployment with zero-maintenance env configuration ([2cb42c7](https://github.com/sarafrika/elimika/commit/2cb42c79f079e9dbb5c7652b8b49af7093fdddcf))
* automate deployment with zero-maintenance env configuration ([3131145](https://github.com/sarafrika/elimika/commit/313114516b08ef6025542ba69f6bea4544243757))
* correct CategoryRepository method signatures and add missing @Query annotation ([07b5a3c](https://github.com/sarafrika/elimika/commit/07b5a3c9ba6c79d4a76264b3f754912929247a0c))
* correct foreign key reference in course_categories table ([a80fd9f](https://github.com/sarafrika/elimika/commit/a80fd9f67114d1b61f5907867ef1e517bb5a05b5))
* correct foreign key reference in course_categories table ([aa2230e](https://github.com/sarafrika/elimika/commit/aa2230eb9cd4d5431b98e6ffcbeb98cf1ef71aa3))
* correct user domain mapping duplicate insertion logic ([740068c](https://github.com/sarafrika/elimika/commit/740068c5f3460910c09a214e3518ae6d97243aef))
* correct user domain mapping duplicate insertion logic ([2c8c6da](https://github.com/sarafrika/elimika/commit/2c8c6daa12779ce177de8bb475785c2349c69c2b))
* correct user domain mapping duplicate insertion logic ([d1002ac](https://github.com/sarafrika/elimika/commit/d1002ac235bc09f3b914813c1a6046a128bb2b7e))
* Cors Error ([20805a1](https://github.com/sarafrika/elimika/commit/20805a10cab5b956ebc12a3cdf74dbe3ccc7ea2a))
* Cors Error ([81cf5b4](https://github.com/sarafrika/elimika/commit/81cf5b4faf569815532eb34435f8c6846de361ab))
* **database:** correct courses and lessons table definitions ([094301a](https://github.com/sarafrika/elimika/commit/094301a8d8c5794c6f6ead4a52c5d0f717eb98d4))
* **db:** Fixing course_categories reference from categories ([b7e9a5a](https://github.com/sarafrika/elimika/commit/b7e9a5aacb6f397c63bbcbc526b4ec6d53fabca4))
* **db:** Fixing course_categories reference from categories ([6f41b3b](https://github.com/sarafrika/elimika/commit/6f41b3b869034fdffe3ba75991d7fd0e6f365fc6))
* **db:** Fixing course_categories reference from categories ([8e69636](https://github.com/sarafrika/elimika/commit/8e69636c00b6b8ea316f497801802a75ef36b739))
* **db:** Fixing course_categories reference from categories ([30b53f8](https://github.com/sarafrika/elimika/commit/30b53f82e52c5d834454b5a72283dfdcd61f1593))
* **db:** Fixing course_categories reference from categories ([728072b](https://github.com/sarafrika/elimika/commit/728072b3c5f5b6eb71c475ba0101e46db2d1ca7b))
* **db:** Fixing course_categories reference from categories ([771e5cd](https://github.com/sarafrika/elimika/commit/771e5cd7b3c3a5d060da4648e9f4ed42c1a14a3b))
* **db:** Fixing course_categories reference from categories ([ae1817a](https://github.com/sarafrika/elimika/commit/ae1817ac7632357a7fd2aea0ceb13b3736ad182a))
* **db:** Fixing course_categories reference from categories ([f363833](https://github.com/sarafrika/elimika/commit/f36383362b382e0947460acda36a3710e476f63d))
* **db:** Fixing course_categories reference from categories ([e5ba11c](https://github.com/sarafrika/elimika/commit/e5ba11cf5e44e68c2c34e259b843435a7b411453))
* **db:** Fixing course_categories reference from categories ([c451fb9](https://github.com/sarafrika/elimika/commit/c451fb9fd9384707e98864038980fce7b76313ff))
* **db:** Fixing course_categories reference from categories ([3406c59](https://github.com/sarafrika/elimika/commit/3406c59e3354a518f11911382984e5a9ccf22662))
* **db:** Fixing course_categories reference from categories ([dc2b4da](https://github.com/sarafrika/elimika/commit/dc2b4dab2b76ae0ea2cee76aaaea2d47281ebd6f))
* **db:** Fixing course_categories reference from categories ([66e8696](https://github.com/sarafrika/elimika/commit/66e86965dd88e6409572b8dfab9930b3ef10e7c6))
* **db:** Fixing course_categories reference from categories ([f456857](https://github.com/sarafrika/elimika/commit/f4568577f74667928f43c0c0c27d8ce2dd45b026))
* **db:** Fixing course_categories reference from categories ([1db182b](https://github.com/sarafrika/elimika/commit/1db182bc9bfc730c13082a79026713c8d178ae78))
* **db:** Fixing course_categories reference from categories ([fd0c775](https://github.com/sarafrika/elimika/commit/fd0c7751a0373cfd3b9061e1569db8d61df2abe6))
* **db:** Fixing course_categories reference from categories ([500c5eb](https://github.com/sarafrika/elimika/commit/500c5eb034a2c00641c8b9b955d702739b479c3e))
* **db:** Fixing course_difficulty_table reference from difficulty_levels ([b13df35](https://github.com/sarafrika/elimika/commit/b13df35f3c8eb39ed197c80f6276087cf804c17a))
* **db:** Fixing lesson_content_types reference from content_types ([05c04fc](https://github.com/sarafrika/elimika/commit/05c04fc52f9785dc6a7b1ca5b7338c5171f31aa3))
* **db:** resolve PostgreSQL immutable function error in partial index ([ae56061](https://github.com/sarafrika/elimika/commit/ae560616de0e4dadee76b4334a491f16a3152dec))
* **db:** Rlashinship enforcement management ([bc7f0b6](https://github.com/sarafrika/elimika/commit/bc7f0b698cb797a0c58c99cc51a60e743f674981))
* enforce UUID-based foreign key relationships across all schemas ([bd11937](https://github.com/sarafrika/elimika/commit/bd1193707101e76e100a1cdf8f3d6a6ce5519280))
* enhance OpenAPI config with multiple auth schemes and streamline server setup ([be8e333](https://github.com/sarafrika/elimika/commit/be8e3333b71f3f0b0054855da6931171fbaf14e1))
* enhance OpenAPI config with multiple auth schemes and streamline server setup ([8ef3a84](https://github.com/sarafrika/elimika/commit/8ef3a849795d20af1aac3d0daf0f53aa676c027d))
* Fixing the Instructor DTO ([5970336](https://github.com/sarafrika/elimika/commit/59703360090f04d3df883f986f047b8adf513152))
* Fixing the Instructor DTO ([1911e09](https://github.com/sarafrika/elimika/commit/1911e0904b4b9d0d900dec2070bfa6017eeff10e))
* Fixing the Instructor DTO ([8e217e1](https://github.com/sarafrika/elimika/commit/8e217e1295ecc359e264576cab3fe633696e54c7))
* Fixing the Instructor DTO ([ed5fe90](https://github.com/sarafrika/elimika/commit/ed5fe904bd14693fb034bd8e6db226fb68fad04e))
* Gender Enum Casting Fix ([1516d2b](https://github.com/sarafrika/elimika/commit/1516d2bb9a4b8b40bcead8760a0895d778733a7b))
* Gender Enum Casting Fix ([a893816](https://github.com/sarafrika/elimika/commit/a893816bdf186a130729323af46c6b76ecbbc6db))
* Gender Enum Casting Fix ([9cbb698](https://github.com/sarafrika/elimika/commit/9cbb698036dffea9d9ae93611cc06480972912ab))
* Gender Enum Casting Fix ([58c3e92](https://github.com/sarafrika/elimika/commit/58c3e920e7b593b6f176c7deb0fadadf68836f7d))
* Grant GITHUB_TOKEN write permissions for Semantic Release ([d97e4f2](https://github.com/sarafrika/elimika/commit/d97e4f2826143c804c618ed6738c92f5326c966b))
* **jpa:** implement equals and hashCode for UserSkillId composite key ([61b5151](https://github.com/sarafrika/elimika/commit/61b5151ab142d2a9a36daada341868b03cb75cfe))
* **jpa:** implement equals and hashCode for UserSkillId composite key ([f448fe6](https://github.com/sarafrika/elimika/commit/f448fe636382a27e309d81cb1327b7b84522e8f2))
* **jpa:** implement equals and hashCode for UserSkillId composite key ([468e5e3](https://github.com/sarafrika/elimika/commit/468e5e3ee940f4838f350cbdbdc19870a1481e14))
* make ContentStatus enum case-insensitive for JSON deserialization ([bf6fdd9](https://github.com/sarafrika/elimika/commit/bf6fdd954b3b6755cad36daa0af1572d9d83e8ce))
* mapping instructor fields at the entity class level ([fc01ba7](https://github.com/sarafrika/elimika/commit/fc01ba78310684aa539cc7c30c582a09f7f4fc12))
* **persistence:** Correct course status enum mapping ([3a85777](https://github.com/sarafrika/elimika/commit/3a8577752164d4c037d42a686706bf039294074d))
* Removal of deprecated property in flyway from the properties yaml file ([c92044a](https://github.com/sarafrika/elimika/commit/c92044af8fb102a8d813ef61677eafc5f4c05c9a))
* removal of direct pushing to main ([330dac2](https://github.com/sarafrika/elimika/commit/330dac2753a022b66c1273cbc1161f87c98654b5))
* Removel of unneeded ([114741b](https://github.com/sarafrika/elimika/commit/114741b8889657cf949efca24940326fc3537c3a))
* Removel of unneeded ([468b05f](https://github.com/sarafrika/elimika/commit/468b05fe94df8488a792f81688d186525e7cfac9))
* replace JPQL with native PostgreSQL queries for ContentType array operations ([224a07f](https://github.com/sarafrika/elimika/commit/224a07f8edcde552091f50c121ca66ec2ed8519e))
* replace RoleRepository.findByUsers_Id with UUID-based query ([b3ab83f](https://github.com/sarafrika/elimika/commit/b3ab83f7f5cab908844b87ac535c1850f216a102))
* resolve entity-database mapping inconsistencies and repository query errors ([fe03b29](https://github.com/sarafrika/elimika/commit/fe03b29f6b77a6e4d9d8fa80d8e07fd2089aeb63))
* **security:** resolve circular dependency in JWT authentication converter ([fbf5a18](https://github.com/sarafrika/elimika/commit/fbf5a18f0f9764188764b0a0549fc8e0f0d5eeb8))
* Tenancy Initialization ([4250736](https://github.com/sarafrika/elimika/commit/4250736a6b0f660cc16720b2c2e99c9bb655a148))
* Update UserFactory to match new UserDTO structure ([d805409](https://github.com/sarafrika/elimika/commit/d805409c96ab4aa93a8461f55743cb1cc84f7370))
* Update UserFactory to match new UserDTO structure ([125d11c](https://github.com/sarafrika/elimika/commit/125d11c24662dc3468b895e51f271363869088d6))
* update UserRepository method to match User entity field name ([b9f9640](https://github.com/sarafrika/elimika/commit/b9f964050eb1badb9826d38b22593ce95ca8585a))
* update UserRepository method to match User entity field name ([fd7c702](https://github.com/sarafrika/elimika/commit/fd7c7024198b3be91566f067d9be74a5629cf90f))
* **user:** Ensuring dob is persisted ([cfd2c35](https://github.com/sarafrika/elimika/commit/cfd2c35d4accacc498af80ab22e3256b871eecb4))


### Code Refactoring

* Replace boolean flags with comprehensive status workflow system ([12c5592](https://github.com/sarafrika/elimika/commit/12c5592ad9e6acea409a40f0ded49898d3da8a12))


### Features

* add admin verification and user domain management ([0a9d7ab](https://github.com/sarafrika/elimika/commit/0a9d7abee73b54f7aae14c9edeb7a33d121ad990))
* add admin verification and user domain management ([5b13da9](https://github.com/sarafrika/elimika/commit/5b13da9257b40bcf1c2ee369476855e17dbeb2c1))
* add admin verification and user domain management ([ac4e809](https://github.com/sarafrika/elimika/commit/ac4e809e7f0e473d1d0cb8d0243b0e99ab9b9731))
* add admin verification and user domain management ([1165a26](https://github.com/sarafrika/elimika/commit/1165a26f351707f7b0063c7e57e44fb83206e198))
* add admin verification and user domain management ([6a76e5c](https://github.com/sarafrika/elimika/commit/6a76e5c52078ba93160940ad3c197c6c1f5eae38))
* Add complete DTO layer for course management system ([62bdc08](https://github.com/sarafrika/elimika/commit/62bdc08c358f8f2209a8901936092bc0b579849b))
* Add complete JPA entity model for course management system ([eb1a3a2](https://github.com/sarafrika/elimika/commit/eb1a3a2f052eb9636a8aa6a0245e8d4718f8d018))
* Add comprehensive course management system database schema ([b2e55c7](https://github.com/sarafrika/elimika/commit/b2e55c73f0e0487317d70b0632eec74b5150c71c))
* Add comprehensive factory classes for entity-DTO conversion ([4253c1b](https://github.com/sarafrika/elimika/commit/4253c1b8deddcfa326c33d6e523a80199ddbbeb9))
* Add comprehensive phone number validation with OpenAPI integration ([4cad414](https://github.com/sarafrika/elimika/commit/4cad414f4eb9077cf47e9ee7d5ac7170758d89d1))
* add factory classes for instructor-related DTOs ([f07ba62](https://github.com/sarafrika/elimika/commit/f07ba627c91c6fe279e3ce0c7ab8243833ba44b7))
* add GlobalExceptionHandler for handling ResourceNotFoundException ([af783f3](https://github.com/sarafrika/elimika/commit/af783f3d0c58e076b6167d12417a74fe77c7f8e2))
* add InstructorDocumentService with comprehensive document management ([ba30b70](https://github.com/sarafrika/elimika/commit/ba30b70941a8208a0d78436d6762324f1fa67ae9))
* add InstructorEducationService with education-specific operations ([4530df0](https://github.com/sarafrika/elimika/commit/4530df053c95f6a79b7934ba2d6a57b93a948543))
* add InstructorExperienceService with experience management ([0511a27](https://github.com/sarafrika/elimika/commit/0511a274731445ad1d82325675baf1bdb8bbe937))
* add InstructorProfessionalMembershipService with membership management ([b4f31c6](https://github.com/sarafrika/elimika/commit/b4f31c63a21395b7cdd2180401f5d06f04fb630a))
* add InstructorSkillService with skill and proficiency management ([844571f](https://github.com/sarafrika/elimika/commit/844571f39f6d9e8a1e6301163e6718556b5650ee))
* add JPA AttributeConverters for course domain enums ([a180dee](https://github.com/sarafrika/elimika/commit/a180dee4819128957db2ea079bc1e37713797e72))
* add manual deployment trigger with configurable options ([b547baf](https://github.com/sarafrika/elimika/commit/b547bafc890d8b4fe22b731dc10bfdfae04e51e6))
* add manual deployment trigger with configurable options ([809ecc3](https://github.com/sarafrika/elimika/commit/809ecc37673bf0b76266537ab430212b5f3d86a1))
* add manual workflow dispatch to Docker build pipeline ([744ec3a](https://github.com/sarafrika/elimika/commit/744ec3a8de89a154a01bb54cbb281b0c25a3ca7f))
* add pageable endpoint to list all users ([971c745](https://github.com/sarafrika/elimika/commit/971c745455f2934312a2b38d5d1d43eff040b465))
* add pageable endpoint to list all users ([cf8763a](https://github.com/sarafrika/elimika/commit/cf8763a68787172cd9e015cb1eba98d21f7e29e4))
* Add service interfaces and repositories for complete LMS architecture ([98834fd](https://github.com/sarafrika/elimika/commit/98834fd0a018646b1f2d2c30cf991250ebb2df0f))
* add status and message to the ResponsePageableDTO ([5e2882c](https://github.com/sarafrika/elimika/commit/5e2882c852cd168106c0c1fa3ef20cf20e5d3391))
* add timestamp field to ResponseDTO ([530b720](https://github.com/sarafrika/elimika/commit/530b7205c54d80e2e706af9ed7ba1354a5fda4f9))
* added course and lesson creation using multipart for files ([a23c9c9](https://github.com/sarafrika/elimika/commit/a23c9c981face8e2d800b28ae43cf7d94846b6c1))
* added course pricing ([f00f2dc](https://github.com/sarafrika/elimika/commit/f00f2dc46249616529dfb1a76f3ecc9b22e8e0d3))
* adding a git action workflow event ([b7f5d3d](https://github.com/sarafrika/elimika/commit/b7f5d3d13a8cff1a0069f54ec992ad136f53acc6))
* Adding the fetch lesson by uuid endpoint ([6ba9c64](https://github.com/sarafrika/elimika/commit/6ba9c642300bdf9a1b96aa2724a5efabdf1a1475))
* addition of semver workflow ([b28ca86](https://github.com/sarafrika/elimika/commit/b28ca86e2e711ee90be1ed2978f545983a3d7248))
* addition of semver workflow ([8c5066a](https://github.com/sarafrika/elimika/commit/8c5066a615baf7238394bad87e334c829e17542e))
* **all:** current changes ([7d971a2](https://github.com/sarafrika/elimika/commit/7d971a23f4dfb5cbb9ca2c55fa60cd40bd41f37d))
* **all:** refactored project structure ([2275b8b](https://github.com/sarafrika/elimika/commit/2275b8bf021de327a8b882f9ace29c2f83fce849))
* **class:** add CRUD operations for class management ([bcb444c](https://github.com/sarafrika/elimika/commit/bcb444c1f20db13cd0ff1e59054ebe8eae9c5964))
* **config:** add env variable placeholders and initial Docker Compose setup ([749a15b](https://github.com/sarafrika/elimika/commit/749a15b6474d2d276b8d40760f8de4ede6898473))
* **config:** add resilient mail configuration to prevent startup failures ([ff45a39](https://github.com/sarafrika/elimika/commit/ff45a392f99bf68e3c1fb41214765ff294978674))
* **course:** add computed properties to CourseDTO with JSON serialization ([385b815](https://github.com/sarafrika/elimika/commit/385b8159a9e7768ec13c457b12f0136759b027fa))
* **course:** add ContentStatus enum with database-compatible mapping ([5b6a045](https://github.com/sarafrika/elimika/commit/5b6a045e28d2d081183e6081682707db4f3064b1))
* **course:** add course model ([f18d37f](https://github.com/sarafrika/elimika/commit/f18d37f872a7ff9ae9297d0ad5474e6fed6d3733))
* **course:** implement `CourseController` and `CourseService` ([3553f7e](https://github.com/sarafrika/elimika/commit/3553f7ee2b435def8ccb2e2144602151e54bf411))
* **course:** Implement searching for courses by name. ([531e664](https://github.com/sarafrika/elimika/commit/531e6645417fa12ce766735d1e477e61f3c1c031))
* **course:** implement update functionality ([a909b78](https://github.com/sarafrika/elimika/commit/a909b78d4753f95f947ef28d0daa86daaa0400a9))
* Create comprehensive DTOs for instructor management system ([a5db4fa](https://github.com/sarafrika/elimika/commit/a5db4fa7a4cfb369f235e7f306b5177fd8e8b574))
* enhance InstructorController with comprehensive CRUD operations for all instructor-related entities ([53f8b1c](https://github.com/sarafrika/elimika/commit/53f8b1c84c37e467b5d229911422b87a2ff2ca13))
* implement complete service layer with 29 Spring Boot service implementations ([1a80f1a](https://github.com/sarafrika/elimika/commit/1a80f1abaf0d520b11ffabbc4c8a330851260fcf))
* implement comprehensive course and lesson management system ([e6cb5cc](https://github.com/sarafrika/elimika/commit/e6cb5cc701d4f94a35bef54c9752d1fe400e63c4))
* implement comprehensive course management API controllers ([656e207](https://github.com/sarafrika/elimika/commit/656e20703f948cd01b541393e3a02825f4081ed0))
* implement comprehensive course management API controllers ([ea4ed2c](https://github.com/sarafrika/elimika/commit/ea4ed2c9a0e58c4824a831f5c400ec3a369d4788))
* implement getCourse and createCourse endpoints in CourseController ([4d6e0eb](https://github.com/sarafrika/elimika/commit/4d6e0eb54c478f1aaf3dc2315ff5a8b7da14630b))
* implement soft delete functionality for Course entity ([ea3ebc2](https://github.com/sarafrika/elimika/commit/ea3ebc256c96d966eaead7cf580ae0fc369f4d76))
* implement soft delete functionality in Course entity ([10d0bc6](https://github.com/sarafrika/elimika/commit/10d0bc6001034690b54e24bd92b333c6adfd862f))
* implement training center and branches functionality ([0df67cc](https://github.com/sarafrika/elimika/commit/0df67cc540dd8aa74c06c9dd3f1afb3212c17baf))
* **instructor:** add CRUD operations for instructor management ([ef231d6](https://github.com/sarafrika/elimika/commit/ef231d6b7469945eb4466d3c62df99ac90e824f8))
* **instructoravailability:** add CRUD operations for instructor availability slot management ([4799cfb](https://github.com/sarafrika/elimika/commit/4799cfb832482d8244f5c412187f13c568a99114))
* **lesson content:** allow fetching of lesson content based on the provided lessonId ([8ee45ea](https://github.com/sarafrika/elimika/commit/8ee45ea8a8e3d962c0035eeabaa96ad752ac941b))
* restructure controllers into hierarchical organization with comprehensive documentation ([8fdbf77](https://github.com/sarafrika/elimika/commit/8fdbf771b01e50967cf63cc920acbb9aad283828))
* **user:** add profile image upload and update user info endpoints ([7d6268c](https://github.com/sarafrika/elimika/commit/7d6268cf75e5aff616c0253c617393d49da72299))
* **user:** add profile image upload and update user info endpoints ([0568a82](https://github.com/sarafrika/elimika/commit/0568a82c18bceaf864f4509290ee28d21510da27))


### BREAKING CHANGES

* Boolean is_published/is_active fields replaced with status enum
* New course and lesson management modules require database schema updates
* Course definition tables and data have been removed
* Course definition tables and data have been removed
* Course definition tables and data have been removed
* User response format now includes domains array
* User response format now includes domains array
* User response format now includes domains array
* User response format now includes domains array
* **security:** Default authorization changed from permitAll to authenticated for non-public endpoints
