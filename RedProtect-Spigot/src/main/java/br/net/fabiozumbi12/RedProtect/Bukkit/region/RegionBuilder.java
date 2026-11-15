/*
 * Copyright (c) 2012-2023 - @FabioZumbi12
 * Last Modified: 02/10/2023 22:14
 *
 * This class is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any
 *  damages arising from the use of this class.
 *
 * Permission is granted to anyone to use this class for any purpose, including commercial plugins, and to alter it and
 * redistribute it freely, subject to the following restrictions:
 * 1 - The origin of this class must not be misrepresented; you must not claim that you wrote the original software. If you
 * use this class in other plugins, an acknowledgment in the plugin documentation would be appreciated but is not required.
 * 2 - Altered source versions must be plainly marked as such, and must not be misrepresented as being the original class.
 * 3 - This notice may not be removed or altered from any source distribution.
 *
 * Esta classe é fornecida "como está", sem qualquer garantia expressa ou implícita. Em nenhum caso os autores serão
 * responsabilizados por quaisquer danos decorrentes do uso desta classe.
 *
 * É concedida permissão a qualquer pessoa para usar esta classe para qualquer finalidade, incluindo plugins pagos, e para
 * alterá-lo e redistribuí-lo livremente, sujeito às seguintes restrições:
 * 1 - A origem desta classe não deve ser deturpada; você não deve afirmar que escreveu a classe original. Se você usar esta
 *  classe em um plugin, uma confirmação de autoria na documentação do plugin será apreciada, mas não é necessária.
 * 2 - Versões de origem alteradas devem ser claramente marcadas como tal e não devem ser deturpadas como sendo a
 * classe original.
 * 3 - Este aviso não pode ser removido ou alterado de qualquer distribuição de origem.
 */

package br.net.fabiozumbi12.RedProtect.Bukkit.region;

import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public abstract class RegionBuilder {

    protected Region r;
    protected boolean committed;

    public RegionBuilder() {
        this.r = null;
        this.committed = false;
    }

    public boolean ready() {
        return this.r != null;
    }

    public boolean isCommitted() {
        return this.committed;
    }

    public Region build() {
        return this.r;
    }

    protected <T> T callSync(Callable<T> task) {
        RedProtect plugin = RedProtect.get();
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }
        if (Bukkit.isPrimaryThread()) {
            try {
                return task.call();
            } catch (Exception ex) {
                plugin.logger.severe("Sync region builder step failed: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            }
        }
        try {
            return Bukkit.getScheduler().callSyncMethod(plugin, task).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            plugin.logger.severe("Interrupted waiting for main-thread region builder step");
            return null;
        } catch (ExecutionException ex) {
            plugin.logger.severe("Main-thread region builder step failed: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    protected void setErrorSign(SignChangeEvent e, String error) {
        callSync(() -> {
            BlockState state = e.getBlock().getState();
            if (state instanceof Sign sign) {
                sign.getSide(Side.FRONT).setLine(0, RedProtect.get().getLanguageManager().get("regionbuilder.signerror"));
                sign.update();
            }
            setError(e.getPlayer(), error);
            return true;
        });
    }

    protected void setError(Player p, String error) {
        RedProtect.get().getLanguageManager().sendMessage(p, RedProtect.get().getLanguageManager().get("regionbuilder.error") + "(" + error + ")");
    }
}
