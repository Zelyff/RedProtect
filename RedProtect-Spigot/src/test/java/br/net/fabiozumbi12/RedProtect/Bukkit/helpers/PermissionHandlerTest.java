package br.net.fabiozumbi12.RedProtect.Bukkit.helpers;

import org.bukkit.permissions.PermissionAttachmentInfo;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class PermissionHandlerTest {

    private static PermissionAttachmentInfo perm(String node, boolean value) {
        PermissionAttachmentInfo info = Mockito.mock(PermissionAttachmentInfo.class);
        when(info.getPermission()).thenReturn(node);
        when(info.getValue()).thenReturn(value);
        return info;
    }

    @Test
    void resolvePermissionNumericLimit_ignoresNegatedAndPicksHighestOrUnlimited() {
        Set<PermissionAttachmentInfo> perms = new LinkedHashSet<>();
        perms.add(perm("redprotect.limits.blocks.5000", false)); // negated must be ignored
        perms.add(perm("redprotect.limits.blocks.800", true));
        perms.add(perm("redprotect.limits.blocks.2000", true));
        perms.add(perm("redprotect.limits.blocks.nether.9999", true)); // world-specific should be filtered out if filter excludes it

        Predicate<String> excludeNether = p -> !p.contains(".nether.");
        int resolved = PermissionHandler.resolvePermissionNumericLimit(perms, "redprotect.limits.blocks.", excludeNether, 500);
        assertEquals(2000, resolved);

        perms.add(perm("redprotect.limits.blocks.-1", true));
        assertEquals(-1, PermissionHandler.resolvePermissionNumericLimit(perms, "redprotect.limits.blocks.", excludeNether, 500));
        assertEquals(-1, PermissionHandler.resolvePermissionNumericLimit(perms, "redprotect.limits.blocks.", excludeNether, -1));
    }
}
