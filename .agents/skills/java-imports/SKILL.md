---
name: java-imports
description: "Google Java import conventions. Use when writing any Java class. No wildcard imports — every import must be declared explicitly, including static imports. Covers regular imports, static imports, Google Java ordering, and Checkstyle enforcement."
---

# Import Conventions

## No Wildcard Imports

Every import must be declared explicitly. Wildcard imports (`*`) are banned in all contexts — regular and static.

Wildcards hide what is actually used, make it impossible to tell which symbol comes from which package, and cause unexpected name clashes when libraries add new members.

```java
// WRONG — wildcard regular import
import java.util.*;

// WRONG — wildcard static import
import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// CORRECT — explicit regular imports
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// CORRECT — explicit static imports
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertSoftly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
```

## Import Ordering

Follow Google Java Checkstyle. The logical groups are:

1. Static imports
2. Non-static imports, ASCII sorted

Do not add blank lines inside either group. The bundled Google Checkstyle configuration is the source of truth.

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.demo.skills.order.OrderService;
import com.demo.skills.order.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
```

## Common Explicit Static Imports — Reference

| Symbol                    | Explicit import                                                                               |
| ------------------------- | --------------------------------------------------------------------------------------------- |
| `given(...)`              | `import static org.mockito.BDDMockito.given;`                                                 |
| `willThrow(...)`          | `import static org.mockito.BDDMockito.willThrow;`                                             |
| `then(...)`               | `import static org.mockito.BDDMockito.then;`                                                  |
| `never()`                 | `import static org.mockito.Mockito.never;`                                                    |
| `any()`                   | `import static org.mockito.ArgumentMatchers.any;`                                             |
| `assertThat(...)`         | `import static org.assertj.core.api.Assertions.assertThat;`                                   |
| `assertThatThrownBy(...)` | `import static org.assertj.core.api.Assertions.assertThatThrownBy;`                           |
| `assertSoftly(...)`       | `import static org.assertj.core.api.Assertions.assertSoftly;`                                 |
| `get(...)` / `post(...)`  | `import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;` etc. |
| `status()`                | `import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;`     |
| `jsonPath(...)`           | `import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;`   |
| `content()`               | `import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;`    |

## Enforcement

Use Google Java Checkstyle from the Checkstyle distribution. Configure the Gradle `checkstyle` task to load `google_checks.xml`.

**IntelliJ IDEA** — Settings → Editor → Code Style → Java → Imports:

- Set "Class count to use import with '\*'" to a very high number (e.g. 999)
- Set "Names count to use static import with '\*'" to 999

Do not add `AvoidStaticImport`; explicit static imports are allowed. Wildcard static imports are banned by Google's `AvoidStarImport`.
