/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.commons;

import com.powsybl.iidm.network.VariantManager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public class ThreadSafeVariantManager implements VariantManager {

    private final ReentrantLock lock = new ReentrantLock();

    private final VariantManager variantManager;

    public ThreadSafeVariantManager(VariantManager variantManager) {
        this.variantManager = variantManager;
    }

    @Override
    public Collection<String> getVariantIds() {
        lock.lock();
        try {
            return variantManager.getVariantIds();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getWorkingVariantId() {
        lock.lock();
        try {
            return variantManager.getWorkingVariantId();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setWorkingVariant(String variantId) {
        lock.lock();
        try {
            variantManager.setWorkingVariant(variantId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void cloneVariant(String sourceVariantId, List<String> sourceVariantIds) {
        lock.lock();
        try {
            variantManager.cloneVariant(sourceVariantId, sourceVariantIds);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void cloneVariant(String sourceVariantId, String targetVariantId) {
        cloneVariant(sourceVariantId, Collections.singletonList(targetVariantId));
    }

    @Override
    public void removeVariant(String s) {
        lock.lock();
        try {
            variantManager.removeVariant(s);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void allowVariantMultiThreadAccess(boolean allow) {
        lock.lock();
        try {
            variantManager.allowVariantMultiThreadAccess(allow);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isVariantMultiThreadAccessAllowed() {
        lock.lock();
        try {
            return variantManager.isVariantMultiThreadAccessAllowed();
        } finally {
            lock.unlock();
        }
    }
}
