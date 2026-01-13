# [2.52.0](https://github.com/sarafrika/elimika/compare/v2.51.1...v2.52.0) (2026-01-13)


### Features

* add user_no identifier with verhoeff check digit for users ([5cb3742](https://github.com/sarafrika/elimika/commit/5cb3742c6a440f66f732ce4d56c13c4d964b0e45))

## [2.51.1](https://github.com/sarafrika/elimika/compare/v2.51.0...v2.51.1) (2026-01-08)


### Bug Fixes

* drop users phone number uniqueness constraint ([6cd9c47](https://github.com/sarafrika/elimika/commit/6cd9c47deaf8db83355e36cfab2e3a7dd4b3c329))

# [2.51.0](https://github.com/sarafrika/elimika/compare/v2.50.0...v2.51.0) (2026-01-07)


### Features

* enroll students when bookings are accepted and paid ([42a94be](https://github.com/sarafrika/elimika/commit/42a94be268d8d659dd3ce6257dba68d4131f776e))
* support training application search by course creator uuid ([8df374d](https://github.com/sarafrika/elimika/commit/8df374dbbf3f0272758510970c28d5dbb3ce3246))

# [2.50.0](https://github.com/sarafrika/elimika/compare/v2.49.0...v2.50.0) (2025-12-19)


### Features

* add instructor acceptance flow for bookings ([030b0c0](https://github.com/sarafrika/elimika/commit/030b0c00653ad5afd8eb8c6f924b64784c8790c6))

# [2.49.0](https://github.com/sarafrika/elimika/compare/v2.48.0...v2.49.0) (2025-12-18)


### Bug Fixes

* allow blocked instructor schedule status ([68812b7](https://github.com/sarafrika/elimika/commit/68812b74d4af6b652fa14f22140df4301552d351))
* keep student full name read-only ([72981c4](https://github.com/sarafrika/elimika/commit/72981c4a153f3148e7f5275799b8a96ca4f293ce))


### Features

* add booking listing and payment request endpoints ([3ec13a0](https://github.com/sarafrika/elimika/commit/3ec13a065876b7b964013637c290c86abfadc1b0))
* include student full name and bio in student responses ([08a8863](https://github.com/sarafrika/elimika/commit/08a886331a5f1d67fc6efdc6cbd8d78181820e87))

# [2.48.0](https://github.com/sarafrika/elimika/compare/v2.47.3...v2.48.0) (2025-12-11)


### Bug Fixes

* add missing list import to admin controller ([c89acc6](https://github.com/sarafrika/elimika/commit/c89acc665883cd02c5f6e0d26ef1986f04b71fbd))
* avoid storing user domains in keycloak provisioning ([b4d2638](https://github.com/sarafrika/elimika/commit/b4d263880824db742b46783a6b14cb7750969371))
* block course creator domain for organisation assignments ([715ec42](https://github.com/sarafrika/elimika/commit/715ec425adea08da58ef785b0c227656695ca7e7))
* include optional imports for domain lookups ([ffa8d74](https://github.com/sarafrika/elimika/commit/ffa8d748900ad6c2377464a1c565b092a3d5050b))


### Features

* add admin user creation flow ([6fb3e21](https://github.com/sarafrika/elimika/commit/6fb3e21bfff4b4f38dd441876a534e5477a9ec78))
* add bulk invitation uploads ([083f5cb](https://github.com/sarafrika/elimika/commit/083f5cb02da31e11e0f87f1b482259f53aa29d2e))
* create organisation users with domain assignment ([8eb6aaa](https://github.com/sarafrika/elimika/commit/8eb6aaa5dc638c044dde38fd737ddf2993d27c08))
* dedupe bulk invitation uploads ([388c676](https://github.com/sarafrika/elimika/commit/388c67601f451a8f7b8f5945aaab93fe561d7a1b))
* expose org-supported domains from user_domain ([63bfee2](https://github.com/sarafrika/elimika/commit/63bfee29c16f6ea1190b34cfbf59a58b1f486564))

## [2.47.3](https://github.com/sarafrika/elimika/compare/v2.47.2...v2.47.3) (2025-12-10)


### Bug Fixes

* add collection import for actuator timers ([a125333](https://github.com/sarafrika/elimika/commit/a1253334db681f1f5b6b3342336c59be712581f4))
* compile actuator metrics lookup ([c9258eb](https://github.com/sarafrika/elimika/commit/c9258eb6b89b34659911282952bffff074fa776c))

## [2.47.2](https://github.com/sarafrika/elimika/compare/v2.47.1...v2.47.2) (2025-12-10)


### Bug Fixes

* avoid promoting organisation admins to platform admins ([9de3e18](https://github.com/sarafrika/elimika/commit/9de3e18a4a3557189a90ed8f9ad6a6db35b242ad))

## [2.47.1](https://github.com/sarafrika/elimika/compare/v2.47.0...v2.47.1) (2025-12-10)


### Bug Fixes

* break timetable to class definition cycle ([4d80012](https://github.com/sarafrika/elimika/commit/4d800122eb2e9e32b1675073035948da2b2ce921))
* break timetable/class definition circular dependency ([f4740c5](https://github.com/sarafrika/elimika/commit/f4740c5f9f4d2decf92c36134e90e8d613dd2fd5))
* resolve timetable wiring without lazy lookup ([191f89c](https://github.com/sarafrika/elimika/commit/191f89cf03c385f8e21d7353532ef5da789d3965))

# [2.47.0](https://github.com/sarafrika/elimika/compare/v2.46.0...v2.47.0) (2025-12-10)


### Bug Fixes

* allow multi-period instructor block requests ([25f3f13](https://github.com/sarafrika/elimika/commit/25f3f13f07f5a63ccc3d3e90c6a81d1931796243))


### Features

* add instructor calendar block endpoint ([58c003c](https://github.com/sarafrika/elimika/commit/58c003c0e3f7b879a63ae542307a38d38ba8f819))
* embed class session templates with conflict handling ([a57cca0](https://github.com/sarafrika/elimika/commit/a57cca0c49def8f6ce64308a4301b4631d724afb))

# [2.46.0](https://github.com/sarafrika/elimika/compare/v2.45.0...v2.46.0) (2025-12-10)


### Bug Fixes

* allow multiple catalogue mappings per course ([5a42cd8](https://github.com/sarafrika/elimika/commit/5a42cd860e0fe48f24cefb7ec2f1dbecf909c63e))


### Features

* add paginated currency listing endpoint ([8b5737f](https://github.com/sarafrika/elimika/commit/8b5737f04f475e44d8c82f9d3b4341a89382606e))
* expose catalogue variant pricing ([b81d884](https://github.com/sarafrika/elimika/commit/b81d884f5adaf35e68818213bb632d1c357cbf90))
* paginate currency listing ([63175f0](https://github.com/sarafrika/elimika/commit/63175f07f86d6a8c9e66f40e762e27ac9c9f6626))

# [2.45.0](https://github.com/sarafrika/elimika/compare/v2.44.0...v2.45.0) (2025-12-10)


### Features

* add bookings with availability holds and payment callbacks ([762a2ad](https://github.com/sarafrika/elimika/commit/762a2add87dabf014edcb59a1285fc722f15ce06))
* support bulk blocking of availability slots ([b74c5db](https://github.com/sarafrika/elimika/commit/b74c5dbbd578fb1d71dec77a216a62287002904b))
* switch block endpoint to bulk payload ([47805b8](https://github.com/sarafrika/elimika/commit/47805b8a688a0d8eae823c5d399d114ae7621710))

# [2.44.0](https://github.com/sarafrika/elimika/compare/v2.43.0...v2.44.0) (2025-12-10)


### Features

* validate active platform currencies for carts ([54c1948](https://github.com/sarafrika/elimika/commit/54c194861835d89e5cc689a76101007dddff7350))

# [2.43.0](https://github.com/sarafrika/elimika/compare/v2.42.0...v2.43.0) (2025-12-10)


### Bug Fixes

* skip inventory gating for digital class variants ([8c2d2d3](https://github.com/sarafrika/elimika/commit/8c2d2d30380af0aaa133617e5ec638559dfa7b81))


### Features

* enforce sku pattern for internal variants ([3e422d8](https://github.com/sarafrika/elimika/commit/3e422d808b082a9237e549c4bea846d87a5e017b))

# [2.42.0](https://github.com/sarafrika/elimika/compare/v2.41.0...v2.42.0) (2025-12-04)


### Bug Fixes

* block admins from self-approving other domains ([d628ce0](https://github.com/sarafrika/elimika/commit/d628ce075477e39a12c729fbd04805fbe8166ed3))
* decouple class capacity checks from timetabling ([88198ae](https://github.com/sarafrika/elimika/commit/88198ae605fe803e845e34880d651a112bdeea4b))


### Features

* enforce class capacity and waitlist flow ([54dc382](https://github.com/sarafrika/elimika/commit/54dc38287b57b041f1288d5bc0c02338a9943e00))

# [2.41.0](https://github.com/sarafrika/elimika/compare/v2.40.0...v2.41.0) (2025-12-03)


### Bug Fixes

* align commerce catalogue search with shared pattern ([f8f743f](https://github.com/sarafrika/elimika/commit/f8f743fe779a487fe642cbb8e61d30b4be2d628e))


### Features

* allow creating catalogue mappings via API ([555e882](https://github.com/sarafrika/elimika/commit/555e8826b27c674cbf177324566ca0fab9523273))
* derive cart region via geoip fallback ([06bf0dd](https://github.com/sarafrika/elimika/commit/06bf0dd4b770e2580241872a4625a914e655fb1f))

# [2.40.0](https://github.com/sarafrika/elimika/compare/v2.39.0...v2.40.0) (2025-12-02)


### Features

* generate readable class variant skus ([efa31ec](https://github.com/sarafrika/elimika/commit/efa31ecd91c413136dbefe2f52dfb4b53be2f6f3))

# [2.39.0](https://github.com/sarafrika/elimika/compare/v2.38.1...v2.39.0) (2025-12-02)


### Bug Fixes

* allow optional user phone numbers ([a9ece75](https://github.com/sarafrika/elimika/commit/a9ece758abce385149e2599105761352dc258484))


### Features

* auto provision catalog items with visibility ([ad17a25](https://github.com/sarafrika/elimika/commit/ad17a25a0b4b064e79fbe33aa3124f7949019785))

## [2.38.1](https://github.com/sarafrika/elimika/compare/v2.38.0...v2.38.1) (2025-11-25)


### Bug Fixes

* populate internal catalog from class definitions ([4e6ff73](https://github.com/sarafrika/elimika/commit/4e6ff7370f6824fadb15f19cb46a130498e5bbc9))

# [2.38.0](https://github.com/sarafrika/elimika/compare/v2.37.0...v2.38.0) (2025-11-25)


### Bug Fixes

* allow scheduling when no availability patterns ([dea5633](https://github.com/sarafrika/elimika/commit/dea5633a4b6605326eafa70caffac624ba16fdde))
* clean availability controller annotations ([af447d1](https://github.com/sarafrika/elimika/commit/af447d1580763d7d872fb18fcbe169515351111c))
* improve API exception messaging ([85c6dc4](https://github.com/sarafrika/elimika/commit/85c6dc4c0e9b320ccd1043b34072eae0c76efebd))
* resolve modulith architecture violations ([27c8417](https://github.com/sarafrika/elimika/commit/27c84179638de98e155b8891b60acc3dbb272634))


### Features

* unify instructor calendar scheduling ([f24722f](https://github.com/sarafrika/elimika/commit/f24722fc6f072b9ca4d584fbf5156f1ccf8be39b))

# [2.37.0](https://github.com/sarafrika/elimika/compare/v2.36.0...v2.37.0) (2025-11-20)


### Bug Fixes

* expose student age gate failures ([1f48fd4](https://github.com/sarafrika/elimika/commit/1f48fd40476bde1a151c498a0cb267194a58c91b))
* resolve underscored search params ([68efc34](https://github.com/sarafrika/elimika/commit/68efc349606f93d2d52ba31cd841d94cb5c1eba9))
* use converter for proficiency levels ([23192c5](https://github.com/sarafrika/elimika/commit/23192c599e5958145b77b9d886d8748d565fd85b))


### Features

* align course creator profiles with instructor data ([34aafb6](https://github.com/sarafrika/elimika/commit/34aafb622d1efd97758990127b5e81cbd6f37180))
* capture course creator qualification records ([403ef5d](https://github.com/sarafrika/elimika/commit/403ef5dba1d2507e4b867bd50e5205368c137ba7))
* enforce training application price floors ([7e4181c](https://github.com/sarafrika/elimika/commit/7e4181c18611ba38be0e62bf818d9d0d5287fe95))
* segment rate cards by delivery modality ([0ec8d5a](https://github.com/sarafrika/elimika/commit/0ec8d5a0a1935e0dc17ebda3bc9284d4f864f366))
* surface keycloak admin events in stats ([46524a6](https://github.com/sarafrika/elimika/commit/46524a6b5ddd8db57636249a8b1e2ce044606ab9))

# [2.36.0](https://github.com/sarafrika/elimika/compare/v2.35.1...v2.36.0) (2025-11-18)


### Features

* email receipts after paid orders ([9e04a6b](https://github.com/sarafrika/elimika/commit/9e04a6bdabd5a059a13fa3c96f6225ba92120cd1))

## [2.35.1](https://github.com/sarafrika/elimika/compare/v2.35.0...v2.35.1) (2025-11-18)


### Bug Fixes

* align instructor reviews migration with class enrollments ([f910174](https://github.com/sarafrika/elimika/commit/f910174bb23db128e94e90eaba6709ec8e1a1874))

# [2.35.0](https://github.com/sarafrika/elimika/compare/v2.34.0...v2.35.0) (2025-11-18)


### Bug Fixes

* break commerce modulith cycle via order completion event ([5002689](https://github.com/sarafrika/elimika/commit/5002689755a922d18158068d549b6c1553ea531e))
* loosen catalog provisioning dependency to satisfy modulith ([56c9380](https://github.com/sarafrika/elimika/commit/56c9380dccd63d860d581481eed7d14977e07909))
* persist instructor skill proficiency enum correctly ([854ec92](https://github.com/sarafrika/elimika/commit/854ec9275c6522588bca24829c18c9eab1ee015b))
* remove course dependency from catalog provisioning ([99e51ff](https://github.com/sarafrika/elimika/commit/99e51ff9fbc9805d88163d30ffb6c5542f8f5014))
* remove paywall spi cycle and update references ([9ef3bc7](https://github.com/sarafrika/elimika/commit/9ef3bc7cd529a4e01c4656c12b7718804f6ed50a))
* resolve modulith architecture violations ([769d677](https://github.com/sarafrika/elimika/commit/769d67785fd48b458ee5a003fd8d532da0c9b264))
* restore instructor review classes ([780d77d](https://github.com/sarafrika/elimika/commit/780d77d7872db2b40fe52571c24b54b4119ab0ef))
* update class catalog listener to new event fields ([be0165f](https://github.com/sarafrika/elimika/commit/be0165fc60247bcb61a3a2c708bb4de874e57f02))


### Features

* add class location coordinates and datetime defaults ([34c2a40](https://github.com/sarafrika/elimika/commit/34c2a40d9773251d8cb35f4a25c68dd319b05009))
* add lesson media uploads, instructor reviews, and private bookings ([056a6e2](https://github.com/sarafrika/elimika/commit/056a6e25cc82198abe9bd3ec694eba45c2be8dee))
* enable internal commerce checkout and paywall ([cc1acda](https://github.com/sarafrika/elimika/commit/cc1acda54f2bbb7fa21bedc414ba18dc8fc6b9ac))
* expose class enrollments to instructors ([7e4f97d](https://github.com/sarafrika/elimika/commit/7e4f97df3313dcc08c631bd080d930f8ebce8cde))

# [2.34.0](https://github.com/sarafrika/elimika/compare/v2.33.0...v2.34.0) (2025-11-10)


### Features

* add system rule governance and age gating ([550959d](https://github.com/sarafrika/elimika/commit/550959da300dbf03d831ee816a3b1d19a6a56c80))
* enforce visibility, session format, and rate cards ([fb7966e](https://github.com/sarafrika/elimika/commit/fb7966edbfe915b82688e647758c03abc48cbe2f))

# [2.33.0](https://github.com/sarafrika/elimika/compare/v2.32.0...v2.33.0) (2025-11-10)


### Features

* enable guardian dashboard access with parent domain and frontend guide ([56e5e59](https://github.com/sarafrika/elimika/commit/56e5e59ddb3b3df47486e77a0687e218b224a63d))

# [2.32.0](https://github.com/sarafrika/elimika/compare/v2.31.1...v2.32.0) (2025-11-05)


### Features

* add admin dashboard activity feed ([040932f](https://github.com/sarafrika/elimika/commit/040932fe02e65f4eb3d0365b421cb50a96c6a8f1))

## [2.31.1](https://github.com/sarafrika/elimika/compare/v2.31.0...v2.31.1) (2025-11-03)


### Bug Fixes

* align instructor document status enum mapping ([cffe068](https://github.com/sarafrika/elimika/commit/cffe06898c6911b680b2cfb805c5bf2fa2dd280e))

# [2.31.0](https://github.com/sarafrika/elimika/compare/v2.30.2...v2.31.0) (2025-11-03)


### Bug Fixes

* block instructor impersonation and hide cancelled schedules ([6da62ea](https://github.com/sarafrika/elimika/commit/6da62ea818d56b2f3feb2c5e51a5332b6f52a2c1))


### Features

* enroll students across all scheduled class sessions ([2be0742](https://github.com/sarafrika/elimika/commit/2be07429e83c063a92f3b6dba21051b734ac0fdc))

## [2.30.2](https://github.com/sarafrika/elimika/compare/v2.30.1...v2.30.2) (2025-10-31)


### Bug Fixes

* add missing spring-boot-starter-actuator dependency ([7946519](https://github.com/sarafrika/elimika/commit/7946519623c3c0ab69d9a07479fbc0cdcac56bef))

## [2.30.1](https://github.com/sarafrika/elimika/compare/v2.30.0...v2.30.1) (2025-10-31)


### Bug Fixes

* resolve thread-safety issue in GenericSpecificationBuilder ([bd6e543](https://github.com/sarafrika/elimika/commit/bd6e54338b2aca4236cc603e530baff80077af2d))

# [2.30.0](https://github.com/sarafrika/elimika/compare/v2.29.3...v2.30.0) (2025-10-31)


### Features

* configure Spring Boot Actuator endpoints ([0eb25be](https://github.com/sarafrika/elimika/commit/0eb25be3094f0fd0ecdb2cb43ca8b18eb6b58ca6))

## [2.29.3](https://github.com/sarafrika/elimika/compare/v2.29.2...v2.29.3) (2025-10-31)


### Bug Fixes

* correct SQL errors in currencies migration ([3388504](https://github.com/sarafrika/elimika/commit/338850469dd60178b8742cd88bd77a04a0169a8c))

## [2.29.2](https://github.com/sarafrika/elimika/compare/v2.29.1...v2.29.2) (2025-10-31)


### Bug Fixes

* correct SQL syntax errors in currencies migration ([23abe91](https://github.com/sarafrika/elimika/commit/23abe91ad30e5c27586545880ab196764193e6e9))

## [2.29.1](https://github.com/sarafrika/elimika/compare/v2.29.0...v2.29.1) (2025-10-31)


### Bug Fixes

* correct Mermaid diagram syntax in ClassAssessmentsFrontendGuide ([b68083f](https://github.com/sarafrika/elimika/commit/b68083f6d78df8ff61c9a483a7cf6c16c1b79926))
* escape single quote in Tongan Pa'anga currency name ([3f08879](https://github.com/sarafrika/elimika/commit/3f0887996172e7f02ce9592b3f6a9577a4cd3882))

# [2.29.0](https://github.com/sarafrika/elimika/compare/v2.28.1...v2.29.0) (2025-10-30)


### Bug Fixes

* improve error message for duplicate course training applications ([d723391](https://github.com/sarafrika/elimika/commit/d72339104d3ad2ee16cfeb275c2797c50c3dcd63))
* resolve circular dependency between notifications and tenancy modules ([8d6e538](https://github.com/sarafrika/elimika/commit/8d6e5389c1ecd7631af735a771e583d3962d2c52))


### Features

* add class-level assessment scheduling with notifications ([f2d5674](https://github.com/sarafrika/elimika/commit/f2d5674b21cd25b4eaacfaffba3268a6baf7da50))
* centralize currency management and enforce platform defaults ([e04a2c9](https://github.com/sarafrika/elimika/commit/e04a2c972926258907c4bf01fd7a07ca75251be1))
* remove lesson duration estimates from lessons ([a5f687d](https://github.com/sarafrika/elimika/commit/a5f687d315fc0d2e4fc5b00dea76c73e3a119940))

## [2.28.1](https://github.com/sarafrika/elimika/compare/v2.28.0...v2.28.1) (2025-10-28)


### Bug Fixes

* make course training application constraints case-insensitive ([95cf511](https://github.com/sarafrika/elimika/commit/95cf511f20fc58c6d1b931d67dd34c6a3ea42176))

# [2.28.0](https://github.com/sarafrika/elimika/compare/v2.27.0...v2.28.0) (2025-10-24)


### Features

* add course training application approvals ([5b7596f](https://github.com/sarafrika/elimika/commit/5b7596f729e48649bc848d60cd5946b1a14f9fc2))

# [2.27.0](https://github.com/sarafrika/elimika/compare/v2.26.4...v2.27.0) (2025-10-24)


### Features

* rename class enrollment persistence to class_enrollments and document utc workflow ([3456fc9](https://github.com/sarafrika/elimika/commit/3456fc995237d5996a49345c3b2750fef02062cf))

## [2.26.4](https://github.com/sarafrika/elimika/compare/v2.26.3...v2.26.4) (2025-10-24)


### Bug Fixes

* achieve full Spring Modulith compliance ([fa26822](https://github.com/sarafrika/elimika/commit/fa26822af332541e1ac9e4b64d06bbeb34987ce1))

## [2.26.3](https://github.com/sarafrika/elimika/compare/v2.26.2...v2.26.3) (2025-10-23)


### Bug Fixes

* correct keycloak server url sample env ([12c8101](https://github.com/sarafrika/elimika/commit/12c81018324adce335ec6b9890c406d640835bb8))

## [2.26.2](https://github.com/sarafrika/elimika/compare/v2.26.1...v2.26.2) (2025-10-20)


### Bug Fixes

* correct Jakarta imports and method names in test files ([86af3c3](https://github.com/sarafrika/elimika/commit/86af3c3036f6fe655ab9baecb54ff60fa2889f17))

## [2.26.1](https://github.com/sarafrika/elimika/compare/v2.26.0...v2.26.1) (2025-10-18)


### Bug Fixes

* properly serialize UserRepresentation in getUserById logging ([663e6aa](https://github.com/sarafrika/elimika/commit/663e6aa4ac2bfe85f93d02acbb6b6a57884603d9))

# [2.26.0](https://github.com/sarafrika/elimika/compare/v2.25.0...v2.26.0) (2025-10-18)


### Bug Fixes

* expose course creator user domain ([eee246f](https://github.com/sarafrika/elimika/commit/eee246f96957b89d6208b265ead28bbaf46d7e2c))


### Features

* add course training requirements and pricing governance ([de09e8b](https://github.com/sarafrika/elimika/commit/de09e8b34455e83b14a263ac0504d59b8fc0aa59))

# [2.25.0](https://github.com/sarafrika/elimika/compare/v2.24.0...v2.25.0) (2025-10-16)


### Bug Fixes

* add missing Keycloak configuration to application.yaml ([df61a7a](https://github.com/sarafrika/elimika/commit/df61a7af54eb628cd373190616481d6fb4252e69))


### Features

* add enhanced search functionality for course module ([86beace](https://github.com/sarafrika/elimika/commit/86beace8480932b95a61f105477c979c73a1b8ec))

# [2.24.0](https://github.com/sarafrika/elimika/compare/v2.23.1...v2.24.0) (2025-10-16)


### Features

* enhance user search with domain filtering and JPA relationships ([c39d1fd](https://github.com/sarafrika/elimika/commit/c39d1fd842789b2b421fc6503a856685610a3433))

## [2.23.1](https://github.com/sarafrika/elimika/compare/v2.23.0...v2.23.1) (2025-10-10)


### Bug Fixes

* remove manual catalog creation endpoint ([c571465](https://github.com/sarafrika/elimika/commit/c5714654ac14a9b40f56515b27aedbb916f1002d))

# [2.23.0](https://github.com/sarafrika/elimika/compare/v2.22.0...v2.23.0) (2025-10-10)


### Features

* enforce commerce paywall before class enrollment ([cae1223](https://github.com/sarafrika/elimika/commit/cae122395eea72fcf265375e4aad15c07172d262))
* enforce commerce paywall before class enrollment ([2d6da05](https://github.com/sarafrika/elimika/commit/2d6da05e01eec582d830df35e6441dce60bac160))
* enforce commerce paywall before class enrollment ([2a791f8](https://github.com/sarafrika/elimika/commit/2a791f8d7b19e8758fce6fb3037ffe21096811f5))

# [2.22.0](https://github.com/sarafrika/elimika/compare/v2.21.0...v2.22.0) (2025-10-10)


### Features

* add request audit logging ([972cbbb](https://github.com/sarafrika/elimika/commit/972cbbbd7b8522f89d7a83d6148f0d8c57d7695a))

# [2.21.0](https://github.com/sarafrika/elimika/compare/v2.20.0...v2.21.0) (2025-10-10)


### Bug Fixes

* normalize commerce DTO JSON mapping and domain checks ([7e9c1e8](https://github.com/sarafrika/elimika/commit/7e9c1e8e03f85da2354ec4b1c69f18548da39096))
* Replace role-based authorization with domain-based security services ([067790f](https://github.com/sarafrika/elimika/commit/067790f245fc6b4650a9e7e90e6a2f8352925a6f))


### Features

* add commerce cart and order APIs backed by Medusa ([8012945](https://github.com/sarafrika/elimika/commit/80129453f48e5ccfb74d25ee5a1a10b2a9ff4e86))

# [2.20.0](https://github.com/sarafrika/elimika/compare/v2.19.0...v2.20.0) (2025-10-03)


### Features

* Implement course ownership authorization for updateCourse endpoint ([7603537](https://github.com/sarafrika/elimika/commit/7603537b1e3c3e31798c2f5955b0f6cbba79f8c5))

# [2.19.0](https://github.com/sarafrika/elimika/compare/v2.18.2...v2.19.0) (2025-10-03)


### Bug Fixes

* resolve compilation errors in availability module ([78f4faa](https://github.com/sarafrika/elimika/commit/78f4faacd403bd32ab3a5eaa01d50db0ddcc7365))


### Features

* Add flexible search endpoint for availability queries ([e5dcb5b](https://github.com/sarafrika/elimika/commit/e5dcb5b5b780f357934660e19327ec07c1b61918))

## [2.18.2](https://github.com/sarafrika/elimika/compare/v2.18.1...v2.18.2) (2025-09-30)


### Bug Fixes

* Drop chk_course_owner constraint before updating courses ([f67f8b8](https://github.com/sarafrika/elimika/commit/f67f8b89b1a3816d416f7cc525183f1bd8121cf6))

## [2.18.1](https://github.com/sarafrika/elimika/compare/v2.18.0...v2.18.1) (2025-09-30)


### Bug Fixes

* Migrate existing course data before removing instructor_uuid column ([2773936](https://github.com/sarafrika/elimika/commit/2773936261cf0fecc33c4c232ca3d8acc4088467))

# [2.18.0](https://github.com/sarafrika/elimika/compare/v2.17.0...v2.18.0) (2025-09-30)


### Features

* Add course creator domain and refactor course ownership ([682c252](https://github.com/sarafrika/elimika/commit/682c25222957ba4b6549b446011048bba1a41c71))

# [2.17.0](https://github.com/sarafrika/elimika/compare/v2.16.0...v2.17.0) (2025-09-16)


### Features

* Add instructor verification system with admin approval workflow ([9541d09](https://github.com/sarafrika/elimika/commit/9541d094497c7699b9b7e85f0b48566429aecf69))
* Add organization verification system with admin approval workflow ([cb7f89a](https://github.com/sarafrika/elimika/commit/cb7f89a9722d235f5001ff7f97a0f6c8a4cd4a41))

# [2.16.0](https://github.com/sarafrika/elimika/compare/v2.15.0...v2.16.0) (2025-09-16)


### Features

* Add @JsonProperty annotations to admin DTOs for snake_case API documentation ([d24d7f7](https://github.com/sarafrika/elimika/commit/d24d7f7223af9260e0d23fa3f0b4576507147786))

# [2.15.0](https://github.com/sarafrika/elimika/compare/v2.14.0...v2.15.0) (2025-09-15)


### Features

* **admin:** Add comprehensive system admin management ([02591de](https://github.com/sarafrika/elimika/commit/02591de22bea7b2ff91418c159aab99cb55d3852))

# [2.14.0](https://github.com/sarafrika/elimika/compare/v2.13.0...v2.14.0) (2025-09-09)


### Features

* **api:** Improve API clarity and documentation ([1feac42](https://github.com/sarafrika/elimika/commit/1feac429941dc0294b0ae4f3abbf730a0f327527))

# [2.13.0](https://github.com/sarafrika/elimika/compare/v2.12.0...v2.13.0) (2025-09-06)


### Features

* **classes:** implement Google Calendar-like recurring class scheduling ([9247070](https://github.com/sarafrika/elimika/commit/9247070be621ef5bdca2acdf98a52beb94e4103d))

# [2.12.0](https://github.com/sarafrika/elimika/compare/v2.11.0...v2.12.0) (2025-09-05)


### Features

* **availability:** add instructor availability management module ([398373e](https://github.com/sarafrika/elimika/commit/398373ea6c3d6fe2d84ca83398fc1afc3d8e903d))
* **timetabling:** add complete timetabling and scheduling module ([f8e5e7b](https://github.com/sarafrika/elimika/commit/f8e5e7b91d5257e14ab94de74fdaf42f094275ba))

# [2.11.0](https://github.com/sarafrika/elimika/compare/v2.10.0...v2.11.0) (2025-09-05)


### Features

* **db:** add audit fields to recurrence patterns table ([19a7e92](https://github.com/sarafrika/elimika/commit/19a7e92e6ba7c1c6f6148c3c9695ab8b1eb44e93))

# [2.10.0](https://github.com/sarafrika/elimika/compare/v2.9.1...v2.10.0) (2025-09-05)


### Features

* **assets:** organize and clean up logo files ([cf05285](https://github.com/sarafrika/elimika/commit/cf0528591e45844f267f47ccffa890db266228cf))
* **classes:** implement comprehensive class definition module ([5802438](https://github.com/sarafrika/elimika/commit/5802438533d7759c8154f03cde5a78d68085c76c))
* **docs:** update notification guide for developers ([6ae897c](https://github.com/sarafrika/elimika/commit/6ae897c119d8829ed104a3ca7eae3a1c9e4c88c2))
* **email:** add Elimika logo and Sarafrika branding to email templates ([1464327](https://github.com/sarafrika/elimika/commit/1464327d51734ae3a0cdb39f9a6a3042c947d861))
* **notifications:** attach full-color logos to all email templates ([2c22704](https://github.com/sarafrika/elimika/commit/2c22704644f1b5e7f75793c376c5b2abd8023057))

## [2.9.1](https://github.com/sarafrika/elimika/compare/v2.9.0...v2.9.1) (2025-09-05)


### Bug Fixes

* **notifications:** resolve DeliveryStatus enum mapping ClassCastException ([5f4fefa](https://github.com/sarafrika/elimika/commit/5f4fefac572aabd3120e3a1f7e47fe2904e74d78))

# [2.9.0](https://github.com/sarafrika/elimika/compare/v2.8.3...v2.9.0) (2025-09-04)


### Features

* **docs:** create admin dashboard development guide ([fdb6a87](https://github.com/sarafrika/elimika/commit/fdb6a87817974c9168b38fa76075df71a73f8793))

## [2.8.3](https://github.com/sarafrika/elimika/compare/v2.8.2...v2.8.3) (2025-09-01)


### Bug Fixes

* **notifications:** Update repository interfaces to match BaseEntity schema ([46180f2](https://github.com/sarafrika/elimika/commit/46180f2c35f64b3f87f5614e550e2c7d9ca0ee8c))

## [2.8.2](https://github.com/sarafrika/elimika/compare/v2.8.1...v2.8.2) (2025-09-01)


### Bug Fixes

* **migrations:** Rename notification schema fixes with correct timestamps ([38dd2eb](https://github.com/sarafrika/elimika/commit/38dd2eb4fda1884ca7fa2e11076e53926d92ce4b))

## [2.8.1](https://github.com/sarafrika/elimika/compare/v2.8.0...v2.8.1) (2025-09-01)


### Bug Fixes

* **notifications:** Fix database schema to match BaseEntity expectations ([ad4ee74](https://github.com/sarafrika/elimika/commit/ad4ee7489ef91c15a578c2cfc559c00e4fcf9b6d))

# [2.8.0](https://github.com/sarafrika/elimika/compare/v2.7.0...v2.8.0) (2025-09-01)


### Features

* **course:** Remove course bundling system completely ([1b22096](https://github.com/sarafrika/elimika/commit/1b22096f6418d5394fdbe15ce40c61ad8e766882))
* **notifications:** Implement Spring Modulith notification system ([8d29d3c](https://github.com/sarafrika/elimika/commit/8d29d3ce7e5b59722bdeebfb3ebff25de25cd43f))

# [2.7.0](https://github.com/sarafrika/elimika/compare/v2.6.0...v2.7.0) (2025-09-01)


### Bug Fixes

* **course:** Resolve compilation errors in course bundling system ([cdac5e6](https://github.com/sarafrika/elimika/commit/cdac5e6670ba0f522b789ff11b3d9fb241a8f96b))


### Features

* **course:** Add course bundling system with independent pricing ([08ef3a5](https://github.com/sarafrika/elimika/commit/08ef3a529a13e75a016630f0ec1a367e6912b3cd))

# [2.6.0](https://github.com/sarafrika/elimika/compare/v2.5.2...v2.6.0) (2025-08-29)


### Bug Fixes

* **course:** Correct AttributeConverter usage in Quiz model ([209511a](https://github.com/sarafrika/elimika/commit/209511a72795140d7a88b317075c25de11b0fe83))
* **tenancy:** Correct method name in InvitationFactory ([90269dc](https://github.com/sarafrika/elimika/commit/90269dc90461dd58f76bef39805e1fd51b327fe2))


### Features

* **invitations:** Add React frontend integration and enhanced invitation system ([2acf6d3](https://github.com/sarafrika/elimika/commit/2acf6d3a74f571cd4078ce53e2a6490bd1305e84))

## [2.5.2](https://github.com/sarafrika/elimika/compare/v2.5.1...v2.5.2) (2025-08-29)


### Bug Fixes

* **email:** Correct email template paths for consistent template resolution ([ebb0520](https://github.com/sarafrika/elimika/commit/ebb05209036f2ba166e7021eefac058ed994dad8))

## [2.5.1](https://github.com/sarafrika/elimika/compare/v2.5.0...v2.5.1) (2025-08-29)


### Bug Fixes

* **tenancy:** Correct InvitationStatusConverter usage and @Builder.Default warning ([748ca51](https://github.com/sarafrika/elimika/commit/748ca516f5a689982a2c0903b52671cdf4964ce4))
* **tenancy:** Resolve ClassCastException for InvitationStatus enum ([80f4db7](https://github.com/sarafrika/elimika/commit/80f4db7277b5be3a2993f58193bf9c7ae96d375e))

# [2.5.0](https://github.com/sarafrika/elimika/compare/v2.4.0...v2.5.0) (2025-08-29)


### Bug Fixes

* Load organization affiliations in getAllUsers ([ca131e5](https://github.com/sarafrika/elimika/commit/ca131e500b800a47ffd2c1570fc2cff33d88d6e3))


### Features

* Add slug and coordinates to OrganisationDTO ([17454e4](https://github.com/sarafrika/elimika/commit/17454e4342767bbbe595b5a7218bd31a4a2b157d))
* Add slug and coordinates to OrganisationDTO ([307b765](https://github.com/sarafrika/elimika/commit/307b7657dc14209c5141438e675ffa5d4b364629))
* **invitations:** Refactor invitation creation API and add documentation ([55ad177](https://github.com/sarafrika/elimika/commit/55ad1778a92b33026b6c8ff45bcac74c2a5b8898))

# [2.4.0](https://github.com/sarafrika/elimika/compare/v2.3.0...v2.4.0) (2025-08-29)


### Features

* Assign organization creator as admin instead of organisation_user ([6bdded6](https://github.com/sarafrika/elimika/commit/6bdded683cae2fce18042d91d1bf24b4c43a63fe))

# [2.3.0](https://github.com/sarafrika/elimika/compare/v2.2.0...v2.3.0) (2025-08-29)


### Features

* Add automatic creator assignment for organization creation ([a0794de](https://github.com/sarafrika/elimika/commit/a0794dedd8b5365478956a4290c8b38fcffc139a))

# [2.2.0](https://github.com/sarafrika/elimika/compare/v2.1.1...v2.2.0) (2025-08-29)


### Bug Fixes

* Resolve compilation errors in user service and invitation entity ([470e68f](https://github.com/sarafrika/elimika/commit/470e68febb1782603c152f3be2c0c1f23afb4505))


### Features

* Add organization affiliations to user profile ([dcd5db2](https://github.com/sarafrika/elimika/commit/dcd5db2ab8952912f3550cd8f60802996fc52b6d))
* Configure Checkstyle with Google checks and add GitHub Actions workflow ([ca261b1](https://github.com/sarafrika/elimika/commit/ca261b1643640bd96c771789b0d0bc50f8690e8d))

## [2.1.1](https://github.com/sarafrika/elimika/compare/v2.1.0...v2.1.1) (2025-08-28)


### Bug Fixes

* Remove obsolete grading level methods from repository ([12c02d3](https://github.com/sarafrika/elimika/commit/12c02d3dd86fcf8bed36cfbd6d252944d111abc7))

# [2.1.0](https://github.com/sarafrika/elimika/compare/v2.0.2...v2.1.0) (2025-08-28)


### Bug Fixes

* Correct DTO and factory for rubric scoring ([5c63ac1](https://github.com/sarafrika/elimika/commit/5c63ac1673f461a7d5b00be4ea94ee31cc9ef4be))
* Resolve compilation errors after decoupling rubric scoring ([2a8109c](https://github.com/sarafrika/elimika/commit/2a8109c374b433784c1b13150b3d4fd12497c989))


### Features

* Decouple rubric scoring from grading levels ([146e4ec](https://github.com/sarafrika/elimika/commit/146e4ec582d91f11a0ba356a29eca20be0d5aa36))

## [2.0.2](https://github.com/sarafrika/elimika/compare/v2.0.1...v2.0.2) (2025-08-28)


### Bug Fixes

* use correct scoring level UUID in matrix cell updates ([5dcdf4d](https://github.com/sarafrika/elimika/commit/5dcdf4d417e594d4bef6fa278992c280ea4aa0bd))

## [2.0.1](https://github.com/sarafrika/elimika/compare/v2.0.0...v2.0.1) (2025-08-27)


### Bug Fixes

* resolve circular dependency between RubricCriteriaService and RubricMatrixService ([dbba3c3](https://github.com/sarafrika/elimika/commit/dbba3c3f8adade2dfc8468442dce9c48a880d49e))

# [2.0.0](https://github.com/sarafrika/elimika/compare/v1.5.3...v2.0.0) (2025-08-27)


### Bug Fixes

* remove non-existent fields from findPopularPublicRubrics query ([8f71887](https://github.com/sarafrika/elimika/commit/8f7188731d71e683ec8708194d8225b9e00e6bb1))
* update TrainingBranchDTO constructor calls for new POC fields ([a3c9fa1](https://github.com/sarafrika/elimika/commit/a3c9fa101789d8308f46f3cd99b7a0b46d3aeeea))


### Code Refactoring

* remove Keycloak organization integration and code generation ([6ebffff](https://github.com/sarafrika/elimika/commit/6ebffff5d3c6de5f7ba62bd31d90de29167f5181))


### Features

* add independent POC fields to training branches ([d6e2f0b](https://github.com/sarafrika/elimika/commit/d6e2f0bfaed97bbcf42909cd402080c0d29c865c))
* implement bidirectional user synchronization with Keycloak ([d4c6bba](https://github.com/sarafrika/elimika/commit/d4c6bba15c9051e41387335877508b1593cbe6fa))


### BREAKING CHANGES

* Organisation entity no longer includes code, domain, or keycloakId fields. Keycloak organization integration has been completely removed.

## [1.5.3](https://github.com/sarafrika/elimika/compare/v1.5.2...v1.5.3) (2025-08-18)


### Bug Fixes

* resolve null pointer exceptions and type casting errors in UserServiceImpl createUser method ([50c2a8b](https://github.com/sarafrika/elimika/commit/50c2a8be524b585016e35ff4e7394f5121e87266))

## [1.5.2](https://github.com/sarafrika/elimika/compare/v1.5.1...v1.5.2) (2025-08-13)


### Bug Fixes

* Add missing update_updated_at_column function for database triggers ([55e1ec0](https://github.com/sarafrika/elimika/commit/55e1ec0304cb44b55c8c6c206e31328fdc64c474))
* **api:** Fixing api response to match existing structure ([cbada50](https://github.com/sarafrika/elimika/commit/cbada50392cd63d2dc9d6cd492024ed1016ec399))

## [1.5.1](https://github.com/sarafrika/elimika/compare/v1.5.0...v1.5.1) (2025-08-13)


### Bug Fixes

* Complete rubric decoupling from courses to enable reusability ([36e3a64](https://github.com/sarafrika/elimika/commit/36e3a643f2bcd768e3fa1edfd7790e874dc1f4fd))

# [1.5.0](https://github.com/sarafrika/elimika/compare/v1.4.1...v1.5.0) (2025-08-13)


### Features

* Update semantic release to depend on build workflow success ([4d6d26a](https://github.com/sarafrika/elimika/commit/4d6d26af44affa49303f04a337343fecdccb1c7f))

## [1.4.1](https://github.com/sarafrika/elimika/compare/v1.4.0...v1.4.1) (2025-08-13)


### Bug Fixes

* Resolve compilation errors in rubric matrix implementation ([242d778](https://github.com/sarafrika/elimika/commit/242d7785130cf1b071c30c542b98b90fb8a49c7c))

# [1.4.0](https://github.com/sarafrika/elimika/compare/v1.3.0...v1.4.0) (2025-08-13)


### Features

* Add rubric matrix support with custom scoring levels ([a8a2d1e](https://github.com/sarafrika/elimika/commit/a8a2d1e2bebbec2568f8635c6d142e5836ba1ea8))

# [1.3.0](https://github.com/sarafrika/elimika/compare/v1.2.0...v1.3.0) (2025-08-13)


### Features

* Add weighted evaluation support to assessment rubrics ([166304c](https://github.com/sarafrika/elimika/commit/166304c85ccbfa768ebe472b14867a36b87fb5f4))

# [1.2.0](https://github.com/sarafrika/elimika/compare/v1.1.0...v1.2.0) (2025-08-13)


### Features

* Add APIs for course rubrics ([df1a142](https://github.com/sarafrika/elimika/commit/df1a142be5b0c4bd495fd37a09d7a433948f2b54))

# [1.1.0](https://github.com/sarafrika/elimika/compare/v1.0.9...v1.1.0) (2025-08-13)


### Features

* Add APIs for course rubrics ([46f2953](https://github.com/sarafrika/elimika/commit/46f2953887c0b453a9d1a9a0db525701fc73ea6b))

## [1.0.9](https://github.com/sarafrika/elimika/compare/v1.0.8...v1.0.9) (2025-08-11)


### Bug Fixes

* **api:** Fixing api response to match existing structure ([e59f27e](https://github.com/sarafrika/elimika/commit/e59f27e7d5b268047b8e5ea29a59f1d6bfbb882d))

## [1.0.8](https://github.com/sarafrika/elimika/compare/v1.0.7...v1.0.8) (2025-08-09)


### Bug Fixes

* **dto:** Added status on training program status ([10e3215](https://github.com/sarafrika/elimika/commit/10e3215920f9460a8970e30738373d111440ebdb))

## [1.0.7](https://github.com/sarafrika/elimika/compare/v1.0.6...v1.0.7) (2025-07-28)


### Bug Fixes

* Profile Image Upload Fix ([981037a](https://github.com/sarafrika/elimika/commit/981037ac7e917fd9f94c8580f74280c1f6a1b4b0))

## [1.0.6](https://github.com/sarafrika/elimika/compare/v1.0.5...v1.0.6) (2025-07-28)


### Bug Fixes

* Profile Image Upload Fix ([3108a27](https://github.com/sarafrika/elimika/commit/3108a27dd208accd3478393f9d881a762e0b63d8))

## [1.0.5](https://github.com/sarafrika/elimika/compare/v1.0.4...v1.0.5) (2025-07-28)


### Bug Fixes

* Check is course has active enrollments during course unpublishing ([0f2e72c](https://github.com/sarafrika/elimika/commit/0f2e72ca9762a68e4622b2fadbe9d885d182968f))

## [1.0.4](https://github.com/sarafrika/elimika/compare/v1.0.3...v1.0.4) (2025-07-27)


### Bug Fixes

* Ensuring update Fields method in UserServiceImpl.java handle all fields from the DTO ([5c5a355](https://github.com/sarafrika/elimika/commit/5c5a355064b9448b59d72cd5075cb40b3672ed62))

## [1.0.3](https://github.com/sarafrika/elimika/compare/v1.0.2...v1.0.3) (2025-07-26)


### Bug Fixes

* provide target type parameter for PostgreSQL CAST function ([b825074](https://github.com/sarafrika/elimika/commit/b825074205584ac15f7c48e58d1a3ac5f34ec57f))

## [1.0.2](https://github.com/sarafrika/elimika/compare/v1.0.1...v1.0.2) (2025-07-26)


### Bug Fixes

* provide target type parameter for PostgreSQL CAST function ([5c9f10e](https://github.com/sarafrika/elimika/commit/5c9f10eb4b6d1e3c81488a766fc289524853638b))

## [1.0.1](https://github.com/sarafrika/elimika/compare/v1.0.0...v1.0.1) (2025-07-26)


### Bug Fixes

* add universal string casting for all field types in LIKE operations ([135d1e5](https://github.com/sarafrika/elimika/commit/135d1e5220a0dc773741af8bf41b56c2d24a64de))

# 1.0.0 (2025-07-26)


* chore!: remove course definitions from database schema and seeded data scripts ([cd34c9a](https://github.com/sarafrika/elimika/commit/cd34c9a718abf5ab09c33bb420c60f1378d2e8a6))
* feat!: implement many-to-many relationship between courses and categories ([918bb1b](https://github.com/sarafrika/elimika/commit/918bb1bdf0cebf59bfff1e779218fec490b5914a))
* refactor!: remove course definitions from database schema ([68bf69a](https://github.com/sarafrika/elimika/commit/68bf69a6468049c1716989098b2bc27b803fc8e6))
* refactor!: remove course definitions from database schema ([034d677](https://github.com/sarafrika/elimika/commit/034d6773e8be2caedae2e4782cb44abf994128ab))
* refactor!: remove redundant course category management endpoints ([b9414df](https://github.com/sarafrika/elimika/commit/b9414dfeebe7a503801c3d8d9752d63fe0c2c9e7))


### Bug Fixes

* .github action workflow fixed ([6e3d60c](https://github.com/sarafrika/elimika/commit/6e3d60c45df4474bee998c3c39facf5a44396050))
* .github action workflow fixed ([4fff73c](https://github.com/sarafrika/elimika/commit/4fff73cd3db3cd4569d54566cc49150802a093f9))
* add @Query annotations for ContentTypeRepository array operations ([601c1d4](https://github.com/sarafrika/elimika/commit/601c1d47c6ebd836c3402f04d16b67b4747b6562))
* add ContentStatus converter and update API documentation ([4f275d1](https://github.com/sarafrika/elimika/commit/4f275d12df969bd297bdad1e59af2f8770d150c1))
* add licence_no field to OrganisationDTO and factory ([f29dd66](https://github.com/sarafrika/elimika/commit/f29dd6647c9bc4348541756ee6e417adf6d5737a))
* add missing status field to TrainingProgram entity and database schema ([552e237](https://github.com/sarafrika/elimika/commit/552e237c0426d96f431087466589a5a177ace053))
* Addition of User Domain ([dc21ee4](https://github.com/sarafrika/elimika/commit/dc21ee4b292e6e5e1e1ccdd8934d96411ebf37d6))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([b9fee53](https://github.com/sarafrika/elimika/commit/b9fee53f0a71e8fab24abb7c03b3aad1ec3dbb55))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([6c91171](https://github.com/sarafrika/elimika/commit/6c9117189e9c6c16783a6b8b5fba6c068251d36b))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([9f0768d](https://github.com/sarafrika/elimika/commit/9f0768d832a09476ce29e8efcae889f950c1330e))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([19c63f5](https://github.com/sarafrika/elimika/commit/19c63f5b967a5238f437e0534b91506e0c417cc5))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([9987067](https://github.com/sarafrika/elimika/commit/9987067051b05e0ee01075b4eb48b666cd73e340))
* **auth:** add Google Tink dependency for EdDSA JWT validation ([107e845](https://github.com/sarafrika/elimika/commit/107e845ad4a6c0aa6bda712b247d6aaa7906c45f))
* **auth:** add Google Tink dependency for EdDSA JWT validation ([35150b9](https://github.com/sarafrika/elimika/commit/35150b9ccd0ba13291a6dc489750af76d96bf331))
* **auth:** add Google Tink dependency for EdDSA JWT validation ([0c5b76a](https://github.com/sarafrika/elimika/commit/0c5b76afff19e03c0c7922d70da0082d6fc3c503))
* automate deployment with zero-maintenance env configuration ([f2db691](https://github.com/sarafrika/elimika/commit/f2db691cec3cbac1be0762a7efedbe6119170a09))
* automate deployment with zero-maintenance env configuration ([e77fd89](https://github.com/sarafrika/elimika/commit/e77fd89dfd87aad22eece3a819df82250a07a21c))
* automate deployment with zero-maintenance env configuration ([2cb42c7](https://github.com/sarafrika/elimika/commit/2cb42c79f079e9dbb5c7652b8b49af7093fdddcf))
* automate deployment with zero-maintenance env configuration ([3131145](https://github.com/sarafrika/elimika/commit/313114516b08ef6025542ba69f6bea4544243757))
* configure SpringDoc to flatten Pageable parameters for Hey API compatibility ([a1451ca](https://github.com/sarafrika/elimika/commit/a1451ca14e38c5d900d79d446d9b3343957d7480))
* configure SpringDoc to flatten Pageable parameters for TypeScript compatibility ([f5684ab](https://github.com/sarafrika/elimika/commit/f5684abd604905e94705107c3f32496b5c008da6))
* correct CategoryRepository method signatures and add missing @Query annotation ([07b5a3c](https://github.com/sarafrika/elimika/commit/07b5a3c9ba6c79d4a76264b3f754912929247a0c))
* correct DECIMAL precision and scale for latitude/longitude columns ([fcebb53](https://github.com/sarafrika/elimika/commit/fcebb53a663674b69f3450782c1e38440e4dbe9f))
* correct foreign key reference in course_categories table ([a80fd9f](https://github.com/sarafrika/elimika/commit/a80fd9f67114d1b61f5907867ef1e517bb5a05b5))
* correct foreign key reference in course_categories table ([aa2230e](https://github.com/sarafrika/elimika/commit/aa2230eb9cd4d5431b98e6ffcbeb98cf1ef71aa3))
* correct user domain mapping duplicate insertion logic ([740068c](https://github.com/sarafrika/elimika/commit/740068c5f3460910c09a214e3518ae6d97243aef))
* correct user domain mapping duplicate insertion logic ([2c8c6da](https://github.com/sarafrika/elimika/commit/2c8c6daa12779ce177de8bb475785c2349c69c2b))
* correct user domain mapping duplicate insertion logic ([d1002ac](https://github.com/sarafrika/elimika/commit/d1002ac235bc09f3b914813c1a6046a128bb2b7e))
* Cors Error ([20805a1](https://github.com/sarafrika/elimika/commit/20805a10cab5b956ebc12a3cdf74dbe3ccc7ea2a))
* Cors Error ([81cf5b4](https://github.com/sarafrika/elimika/commit/81cf5b4faf569815532eb34435f8c6846de361ab))
* **courses:** resolve constraint violation when unpublishing courses ([0ae6105](https://github.com/sarafrika/elimika/commit/0ae61052e6d1e4e201432c0918d853ff5691e598))
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
* Documentation metadata fixing on StudentDTO ([2b7abee](https://github.com/sarafrika/elimika/commit/2b7abeee553f42b83d6a044b3922fcc7010d822e))
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
* remove invalid string defaultValue from boolean fields in DTOs ([4774e5c](https://github.com/sarafrika/elimika/commit/4774e5c92b5c9cc0d2f9a9a83af5511c1d0a2473))
* Remove unused and decalred JPA methods from COurse Repository ([dc8cbbd](https://github.com/sarafrika/elimika/commit/dc8cbbdd72b15d250dc62430bc6699ec105a40d3))
* Removel of unneeded ([114741b](https://github.com/sarafrika/elimika/commit/114741b8889657cf949efca24940326fc3537c3a))
* Removel of unneeded ([468b05f](https://github.com/sarafrika/elimika/commit/468b05fe94df8488a792f81688d186525e7cfac9))
* replace JPQL with native PostgreSQL queries for ContentType array operations ([224a07f](https://github.com/sarafrika/elimika/commit/224a07f8edcde552091f50c121ca66ec2ed8519e))
* replace RoleRepository.findByUsers_Id with UUID-based query ([b3ab83f](https://github.com/sarafrika/elimika/commit/b3ab83f7f5cab908844b87ac535c1850f216a102))
* resolve entity-database mapping inconsistencies and repository query errors ([fe03b29](https://github.com/sarafrika/elimika/commit/fe03b29f6b77a6e4d9d8fa80d8e07fd2089aeb63))
* resolve PostgreSQL type casting errors in string operations ([887918f](https://github.com/sarafrika/elimika/commit/887918fbdab02665a4c0bbe867bbe226b417e1c6))
* **search:** Rectify dynamic query parameter parsing and documentation ([14d7cff](https://github.com/sarafrika/elimika/commit/14d7cffb4246e96bb0778344fa2bee0954cf15d6))
* **security:** resolve circular dependency in JWT authentication converter ([fbf5a18](https://github.com/sarafrika/elimika/commit/fbf5a18f0f9764188764b0a0549fc8e0f0d5eeb8))
* Tenancy Initialization ([4250736](https://github.com/sarafrika/elimika/commit/4250736a6b0f660cc16720b2c2e99c9bb655a148))
* Update UserFactory to match new UserDTO structure ([d805409](https://github.com/sarafrika/elimika/commit/d805409c96ab4aa93a8461f55743cb1cc84f7370))
* Update UserFactory to match new UserDTO structure ([125d11c](https://github.com/sarafrika/elimika/commit/125d11c24662dc3468b895e51f271363869088d6))
* update UserRepository method to match User entity field name ([b9f9640](https://github.com/sarafrika/elimika/commit/b9f964050eb1badb9826d38b22593ce95ca8585a))
* update UserRepository method to match User entity field name ([fd7c702](https://github.com/sarafrika/elimika/commit/fd7c7024198b3be91566f067d9be74a5629cf90f))
* update website validation pattern to handle optional empty values ([6b068da](https://github.com/sarafrika/elimika/commit/6b068da53e9cd7dec3d3c832568ebcbbca0c473f))
* use exact matching for non-string fields in like operations ([b1b6426](https://github.com/sarafrika/elimika/commit/b1b6426d978b236d4aa1d17ced54f0ca7a031b05))
* **user:** Ensuring dob is persisted ([cfd2c35](https://github.com/sarafrika/elimika/commit/cfd2c35d4accacc498af80ab22e3256b871eecb4))


### Code Refactoring

* Replace boolean flags with comprehensive status workflow system ([12c5592](https://github.com/sarafrika/elimika/commit/12c5592ad9e6acea409a40f0ded49898d3da8a12))


### Features

* add admin verification and user domain management ([0a9d7ab](https://github.com/sarafrika/elimika/commit/0a9d7abee73b54f7aae14c9edeb7a33d121ad990))
* add admin verification and user domain management ([5b13da9](https://github.com/sarafrika/elimika/commit/5b13da9257b40bcf1c2ee369476855e17dbeb2c1))
* add admin verification and user domain management ([ac4e809](https://github.com/sarafrika/elimika/commit/ac4e809e7f0e473d1d0cb8d0243b0e99ab9b9731))
* add admin verification and user domain management ([1165a26](https://github.com/sarafrika/elimika/commit/1165a26f351707f7b0063c7e57e44fb83206e198))
* add admin verification and user domain management ([6a76e5c](https://github.com/sarafrika/elimika/commit/6a76e5c52078ba93160940ad3c197c6c1f5eae38))
* add automatic full_name generation for students table ([e99de41](https://github.com/sarafrika/elimika/commit/e99de41de8f842a2ab288b7614c15807cec4d5a8))
* Add complete DTO layer for course management system ([62bdc08](https://github.com/sarafrika/elimika/commit/62bdc08c358f8f2209a8901936092bc0b579849b))
* Add complete JPA entity model for course management system ([eb1a3a2](https://github.com/sarafrika/elimika/commit/eb1a3a2f052eb9636a8aa6a0245e8d4718f8d018))
* Add comprehensive course management system database schema ([b2e55c7](https://github.com/sarafrika/elimika/commit/b2e55c73f0e0487317d70b0632eec74b5150c71c))
* Add comprehensive factory classes for entity-DTO conversion ([4253c1b](https://github.com/sarafrika/elimika/commit/4253c1b8deddcfa326c33d6e523a80199ddbbeb9))
* Add comprehensive phone number validation with OpenAPI integration ([4cad414](https://github.com/sarafrika/elimika/commit/4cad414f4eb9077cf47e9ee7d5ac7170758d89d1))
* add custom URL validator with OpenAPI schema integration ([0055523](https://github.com/sarafrika/elimika/commit/00555234089180aca3bd3cfc9242d62104174683))
* add event listener for automatic user domain assignment ([fc44c9a](https://github.com/sarafrika/elimika/commit/fc44c9ac00a2616a7da80794a1580dc69fa59a44))
* add factory classes for instructor-related DTOs ([f07ba62](https://github.com/sarafrika/elimika/commit/f07ba627c91c6fe279e3ce0c7ab8243833ba44b7))
* add GlobalExceptionHandler for handling ResourceNotFoundException ([af783f3](https://github.com/sarafrika/elimika/commit/af783f3d0c58e076b6167d12417a74fe77c7f8e2))
* add InstructorDocumentService with comprehensive document management ([ba30b70](https://github.com/sarafrika/elimika/commit/ba30b70941a8208a0d78436d6762324f1fa67ae9))
* add InstructorEducationService with education-specific operations ([4530df0](https://github.com/sarafrika/elimika/commit/4530df053c95f6a79b7934ba2d6a57b93a948543))
* add InstructorExperienceService with experience management ([0511a27](https://github.com/sarafrika/elimika/commit/0511a274731445ad1d82325675baf1bdb8bbe937))
* add InstructorProfessionalMembershipService with membership management ([b4f31c6](https://github.com/sarafrika/elimika/commit/b4f31c63a21395b7cdd2180401f5d06f04fb630a))
* add InstructorSkillService with skill and proficiency management ([844571f](https://github.com/sarafrika/elimika/commit/844571f39f6d9e8a1e6301163e6718556b5650ee))
* add JPA AttributeConverters for course domain enums ([a180dee](https://github.com/sarafrika/elimika/commit/a180dee4819128957db2ea079bc1e37713797e72))
* add JsonProperty name resolution for validation errors ([a7e0eeb](https://github.com/sarafrika/elimika/commit/a7e0eeba5b1b19291178b349ef4ced0bb3993bd6))
* add manual deployment trigger with configurable options ([b547baf](https://github.com/sarafrika/elimika/commit/b547bafc890d8b4fe22b731dc10bfdfae04e51e6))
* add manual deployment trigger with configurable options ([809ecc3](https://github.com/sarafrika/elimika/commit/809ecc37673bf0b76266537ab430212b5f3d86a1))
* add manual workflow dispatch to Docker build pipeline ([744ec3a](https://github.com/sarafrika/elimika/commit/744ec3a8de89a154a01bb54cbb281b0c25a3ca7f))
* add pageable endpoint to list all users ([971c745](https://github.com/sarafrika/elimika/commit/971c745455f2934312a2b38d5d1d43eff040b465))
* add pageable endpoint to list all users ([cf8763a](https://github.com/sarafrika/elimika/commit/cf8763a68787172cd9e015cb1eba98d21f7e29e4))
* add ProficiencyLevel enum converter for JPA persistence ([3ac3fc3](https://github.com/sarafrika/elimika/commit/3ac3fc39e8acb31aa1b87f7590aa32ffe525b202))
* Add service interfaces and repositories for complete LMS architecture ([98834fd](https://github.com/sarafrika/elimika/commit/98834fd0a018646b1f2d2c30cf991250ebb2df0f))
* add smart course unpublish and lifecycle management ([ac0a57c](https://github.com/sarafrika/elimika/commit/ac0a57cb9702aa8bb189acf734dbe43dd41c4986))
* add status and message to the ResponsePageableDTO ([5e2882c](https://github.com/sarafrika/elimika/commit/5e2882c852cd168106c0c1fa3ef20cf20e5d3391))
* add timestamp field to ResponseDTO ([530b720](https://github.com/sarafrika/elimika/commit/530b7205c54d80e2e706af9ed7ba1354a5fda4f9))
* added course and lesson creation using multipart for files ([a23c9c9](https://github.com/sarafrika/elimika/commit/a23c9c981face8e2d800b28ae43cf7d94846b6c1))
* added course pricing ([f00f2dc](https://github.com/sarafrika/elimika/commit/f00f2dc46249616529dfb1a76f3ecc9b22e8e0d3))
* Added validation of phone numbers when registering Student Bio Information ([fb04db8](https://github.com/sarafrika/elimika/commit/fb04db8a5515b3bbc7fa703dbcf6f88508050f4c))
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
* externalize storage folder configuration to properties ([e5c2611](https://github.com/sarafrika/elimika/commit/e5c2611fa25ce0611b23f1b26bbe984b5856405f))
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
* **storage:** implement folder-based storage for profile images and course media ([005d1e8](https://github.com/sarafrika/elimika/commit/005d1e8ac811ae3dc3d9e7c2fe2d04dbe91c24d5))
* **user:** add profile image upload and update user info endpoints ([7d6268c](https://github.com/sarafrika/elimika/commit/7d6268cf75e5aff616c0253c617393d49da72299))
* **user:** add profile image upload and update user info endpoints ([0568a82](https://github.com/sarafrika/elimika/commit/0568a82c18bceaf864f4509290ee28d21510da27))


### BREAKING CHANGES

* Remove unnecessary category management endpoints in favor of standard CRUD operations

Removed endpoints:
- POST /api/v1/courses/{uuid}/categories/{categoryUuid} (add single category)
- POST /api/v1/courses/{uuid}/categories (add multiple categories)
- PUT /api/v1/courses/{uuid}/categories (replace all categories)

Rationale:
- Course creation and updates already handle multiple categories via category_uuids field
- Standard CRUD operations (POST/PUT /courses) provide all necessary functionality
- Eliminates redundant API surface and reduces maintenance overhead
- Simplifies client integration by providing single source of truth for category management

Kept essential endpoints:
- GET /api/v1/courses/{uuid}/categories (view current categories)
- DELETE /api/v1/courses/{uuid}/categories/{categoryUuid} (remove specific category)
- DELETE /api/v1/courses/{uuid}/categories (remove all categories)

Migration guide:
- Use POST /api/v1/courses with category_uuids for course creation with categories
- Use PUT /api/v1/courses/{uuid} with category_uuids for updating course categories
- Cleanup operations still available via DELETE endpoints
* Replace single category_uuid field with category_uuids array in Course API

- Add course_category_mappings junction table with proper indexing
- Remove deprecated category_uuid field from courses table and entities
- Implement CourseCategoryService for relationship management
- Add category_uuids field to CourseDTO for multiple category assignment
- Add computed category_names field for readable category display
- Update CourseController with category management endpoints
- Migrate existing category data to new junction table structure
- Add comprehensive API documentation and usage examples
* **storage:** Profile images now stored in profile_images/ folder instead of root directory
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

# [4.7.0](https://github.com/sarafrika/elimika/compare/v4.6.0...v4.7.0) (2025-07-26)


### Bug Fixes

* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([b9fee53](https://github.com/sarafrika/elimika/commit/b9fee53f0a71e8fab24abb7c03b3aad1ec3dbb55))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([6c91171](https://github.com/sarafrika/elimika/commit/6c9117189e9c6c16783a6b8b5fba6c068251d36b))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([9f0768d](https://github.com/sarafrika/elimika/commit/9f0768d832a09476ce29e8efcae889f950c1330e))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([19c63f5](https://github.com/sarafrika/elimika/commit/19c63f5b967a5238f437e0534b91506e0c417cc5))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([9987067](https://github.com/sarafrika/elimika/commit/9987067051b05e0ee01075b4eb48b666cd73e340))
* resolve PostgreSQL type casting errors in string operations ([887918f](https://github.com/sarafrika/elimika/commit/887918fbdab02665a4c0bbe867bbe226b417e1c6))
* **search:** Rectify dynamic query parameter parsing and documentation ([14d7cff](https://github.com/sarafrika/elimika/commit/14d7cffb4246e96bb0778344fa2bee0954cf15d6))


### Features

* add event listener for automatic user domain assignment ([fc44c9a](https://github.com/sarafrika/elimika/commit/fc44c9ac00a2616a7da80794a1580dc69fa59a44))

# [4.7.0](https://github.com/sarafrika/elimika/compare/v4.6.0...v4.7.0) (2025-07-25)


### Bug Fixes

* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([b9fee53](https://github.com/sarafrika/elimika/commit/b9fee53f0a71e8fab24abb7c03b3aad1ec3dbb55))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([6c91171](https://github.com/sarafrika/elimika/commit/6c9117189e9c6c16783a6b8b5fba6c068251d36b))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([9f0768d](https://github.com/sarafrika/elimika/commit/9f0768d832a09476ce29e8efcae889f950c1330e))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([19c63f5](https://github.com/sarafrika/elimika/commit/19c63f5b967a5238f437e0534b91506e0c417cc5))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([9987067](https://github.com/sarafrika/elimika/commit/9987067051b05e0ee01075b4eb48b666cd73e340))
* **search:** Rectify dynamic query parameter parsing and documentation ([14d7cff](https://github.com/sarafrika/elimika/commit/14d7cffb4246e96bb0778344fa2bee0954cf15d6))


### Features

* add event listener for automatic user domain assignment ([fc44c9a](https://github.com/sarafrika/elimika/commit/fc44c9ac00a2616a7da80794a1580dc69fa59a44))

# [4.7.0](https://github.com/sarafrika/elimika/compare/v4.6.0...v4.7.0) (2025-07-25)


### Bug Fixes

* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([6c91171](https://github.com/sarafrika/elimika/commit/6c9117189e9c6c16783a6b8b5fba6c068251d36b))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([9f0768d](https://github.com/sarafrika/elimika/commit/9f0768d832a09476ce29e8efcae889f950c1330e))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([19c63f5](https://github.com/sarafrika/elimika/commit/19c63f5b967a5238f437e0534b91506e0c417cc5))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([9987067](https://github.com/sarafrika/elimika/commit/9987067051b05e0ee01075b4eb48b666cd73e340))
* **search:** Rectify dynamic query parameter parsing and documentation ([14d7cff](https://github.com/sarafrika/elimika/commit/14d7cffb4246e96bb0778344fa2bee0954cf15d6))


### Features

* add event listener for automatic user domain assignment ([fc44c9a](https://github.com/sarafrika/elimika/commit/fc44c9ac00a2616a7da80794a1580dc69fa59a44))

# [4.7.0](https://github.com/sarafrika/elimika/compare/v4.6.0...v4.7.0) (2025-07-25)


### Bug Fixes

* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([19c63f5](https://github.com/sarafrika/elimika/commit/19c63f5b967a5238f437e0534b91506e0c417cc5))
* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([9987067](https://github.com/sarafrika/elimika/commit/9987067051b05e0ee01075b4eb48b666cd73e340))
* **search:** Rectify dynamic query parameter parsing and documentation ([14d7cff](https://github.com/sarafrika/elimika/commit/14d7cffb4246e96bb0778344fa2bee0954cf15d6))


### Features

* add event listener for automatic user domain assignment ([fc44c9a](https://github.com/sarafrika/elimika/commit/fc44c9ac00a2616a7da80794a1580dc69fa59a44))

# [4.7.0](https://github.com/sarafrika/elimika/compare/v4.6.0...v4.7.0) (2025-07-25)


### Bug Fixes

* adminVerified was a primitive type, converted it to a wrapper class to allow for null values ([9987067](https://github.com/sarafrika/elimika/commit/9987067051b05e0ee01075b4eb48b666cd73e340))
* **search:** Rectify dynamic query parameter parsing and documentation ([14d7cff](https://github.com/sarafrika/elimika/commit/14d7cffb4246e96bb0778344fa2bee0954cf15d6))


### Features

* add event listener for automatic user domain assignment ([fc44c9a](https://github.com/sarafrika/elimika/commit/fc44c9ac00a2616a7da80794a1580dc69fa59a44))

# [4.7.0](https://github.com/sarafrika/elimika/compare/v4.6.0...v4.7.0) (2025-07-25)


### Bug Fixes

* **search:** Rectify dynamic query parameter parsing and documentation ([14d7cff](https://github.com/sarafrika/elimika/commit/14d7cffb4246e96bb0778344fa2bee0954cf15d6))


### Features

* add event listener for automatic user domain assignment ([fc44c9a](https://github.com/sarafrika/elimika/commit/fc44c9ac00a2616a7da80794a1580dc69fa59a44))

## [4.6.1](https://github.com/sarafrika/elimika/compare/v4.6.0...v4.6.1) (2025-07-24)


### Bug Fixes

* **search:** Rectify dynamic query parameter parsing and documentation ([14d7cff](https://github.com/sarafrika/elimika/commit/14d7cffb4246e96bb0778344fa2bee0954cf15d6))

# [4.6.0](https://github.com/sarafrika/elimika/compare/v4.5.4...v4.6.0) (2025-07-22)


### Features

* add custom URL validator with OpenAPI schema integration ([0055523](https://github.com/sarafrika/elimika/commit/00555234089180aca3bd3cfc9242d62104174683))

## [4.5.4](https://github.com/sarafrika/elimika/compare/v4.5.3...v4.5.4) (2025-07-22)


### Bug Fixes

* update website validation pattern to handle optional empty values ([6b068da](https://github.com/sarafrika/elimika/commit/6b068da53e9cd7dec3d3c832568ebcbbca0c473f))

## [4.5.3](https://github.com/sarafrika/elimika/compare/v4.5.2...v4.5.3) (2025-07-22)


### Bug Fixes

* configure SpringDoc to flatten Pageable parameters for Hey API compatibility ([a1451ca](https://github.com/sarafrika/elimika/commit/a1451ca14e38c5d900d79d446d9b3343957d7480))

## [4.5.2](https://github.com/sarafrika/elimika/compare/v4.5.1...v4.5.2) (2025-07-22)


### Bug Fixes

* configure SpringDoc to flatten Pageable parameters for TypeScript compatibility ([f5684ab](https://github.com/sarafrika/elimika/commit/f5684abd604905e94705107c3f32496b5c008da6))

## [4.5.1](https://github.com/sarafrika/elimika/compare/v4.5.0...v4.5.1) (2025-07-22)


### Bug Fixes

* remove invalid string defaultValue from boolean fields in DTOs ([4774e5c](https://github.com/sarafrika/elimika/commit/4774e5c92b5c9cc0d2f9a9a83af5511c1d0a2473))

# [4.5.0](https://github.com/sarafrika/elimika/compare/v4.4.0...v4.5.0) (2025-07-21)


### Features

* add ProficiencyLevel enum converter for JPA persistence ([3ac3fc3](https://github.com/sarafrika/elimika/commit/3ac3fc39e8acb31aa1b87f7590aa32ffe525b202))

# [4.4.0](https://github.com/sarafrika/elimika/compare/v4.3.1...v4.4.0) (2025-07-21)


### Features

* add automatic full_name generation for students table ([e99de41](https://github.com/sarafrika/elimika/commit/e99de41de8f842a2ab288b7614c15807cec4d5a8))

## [4.3.1](https://github.com/sarafrika/elimika/compare/v4.3.0...v4.3.1) (2025-07-21)


### Bug Fixes

* **courses:** resolve constraint violation when unpublishing courses ([0ae6105](https://github.com/sarafrika/elimika/commit/0ae61052e6d1e4e201432c0918d853ff5691e598))

# [4.3.0](https://github.com/sarafrika/elimika/compare/v4.2.0...v4.3.0) (2025-07-18)


### Features

* externalize storage folder configuration to properties ([e5c2611](https://github.com/sarafrika/elimika/commit/e5c2611fa25ce0611b23f1b26bbe984b5856405f))

# [4.2.0](https://github.com/sarafrika/elimika/compare/v4.1.1...v4.2.0) (2025-07-18)


### Features

* add JsonProperty name resolution for validation errors ([a7e0eeb](https://github.com/sarafrika/elimika/commit/a7e0eeba5b1b19291178b349ef4ced0bb3993bd6))

## [4.1.1](https://github.com/sarafrika/elimika/compare/v4.1.0...v4.1.1) (2025-07-18)


### Bug Fixes

* Documentation metadata fixing on StudentDTO ([2b7abee](https://github.com/sarafrika/elimika/commit/2b7abeee553f42b83d6a044b3922fcc7010d822e))

# [4.1.0](https://github.com/sarafrika/elimika/compare/v4.0.0...v4.1.0) (2025-07-18)


### Features

* Added validation of phone numbers when registering Student Bio Information ([fb04db8](https://github.com/sarafrika/elimika/commit/fb04db8a5515b3bbc7fa703dbcf6f88508050f4c))

# [4.0.0](https://github.com/sarafrika/elimika/compare/v3.1.1...v4.0.0) (2025-07-18)


* refactor!: remove redundant course category management endpoints ([b9414df](https://github.com/sarafrika/elimika/commit/b9414dfeebe7a503801c3d8d9752d63fe0c2c9e7))


### BREAKING CHANGES

* Remove unnecessary category management endpoints in favor of standard CRUD operations

Removed endpoints:
- POST /api/v1/courses/{uuid}/categories/{categoryUuid} (add single category)
- POST /api/v1/courses/{uuid}/categories (add multiple categories)
- PUT /api/v1/courses/{uuid}/categories (replace all categories)

Rationale:
- Course creation and updates already handle multiple categories via category_uuids field
- Standard CRUD operations (POST/PUT /courses) provide all necessary functionality
- Eliminates redundant API surface and reduces maintenance overhead
- Simplifies client integration by providing single source of truth for category management

Kept essential endpoints:
- GET /api/v1/courses/{uuid}/categories (view current categories)
- DELETE /api/v1/courses/{uuid}/categories/{categoryUuid} (remove specific category)
- DELETE /api/v1/courses/{uuid}/categories (remove all categories)

Migration guide:
- Use POST /api/v1/courses with category_uuids for course creation with categories
- Use PUT /api/v1/courses/{uuid} with category_uuids for updating course categories
- Cleanup operations still available via DELETE endpoints

## [3.1.1](https://github.com/sarafrika/elimika/compare/v3.1.0...v3.1.1) (2025-07-18)


### Bug Fixes

* Remove unused and decalred JPA methods from COurse Repository ([dc8cbbd](https://github.com/sarafrika/elimika/commit/dc8cbbdd72b15d250dc62430bc6699ec105a40d3))

# [3.1.0](https://github.com/sarafrika/elimika/compare/v3.0.0...v3.1.0) (2025-07-18)


### Features

* add smart course unpublish and lifecycle management ([ac0a57c](https://github.com/sarafrika/elimika/commit/ac0a57cb9702aa8bb189acf734dbe43dd41c4986))

# [3.0.0](https://github.com/sarafrika/elimika/compare/v2.0.0...v3.0.0) (2025-07-18)


* feat!: implement many-to-many relationship between courses and categories ([918bb1b](https://github.com/sarafrika/elimika/commit/918bb1bdf0cebf59bfff1e779218fec490b5914a))


### BREAKING CHANGES

* Replace single category_uuid field with category_uuids array in Course API

- Add course_category_mappings junction table with proper indexing
- Remove deprecated category_uuid field from courses table and entities
- Implement CourseCategoryService for relationship management
- Add category_uuids field to CourseDTO for multiple category assignment
- Add computed category_names field for readable category display
- Update CourseController with category management endpoints
- Migrate existing category data to new junction table structure
- Add comprehensive API documentation and usage examples

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
