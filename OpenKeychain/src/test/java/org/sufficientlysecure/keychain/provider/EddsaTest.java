/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.provider;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import android.app.Application;

import org.bouncycastle.bcpg.sig.KeyFlags;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.util.Util;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyOperation;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;


@SuppressWarnings("WeakerAccess")
@RunWith(KeychainTestRunner.class)
public class EddsaTest {
    private ProviderHelper providerHelper;
    private Application context;


    @BeforeClass
    public static void setUpOnce() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.application;
        providerHelper = new ProviderHelper(context);

    }

    @Test
    public void testGpgSampleSignature() throws Exception {
        // key from GnuPG's test suite, sample msg generated using GnuPG v2.1.18
        UncachedKeyRing ring = loadKeyringFromResource("/test-keys/eddsa-sample-1-pub.asc");

        byte[] signedText = readBytesFromResource("/test-keys/eddsa-sample-msg.asc");
        PgpDecryptVerifyInputParcel pgpDecryptVerifyInputParcel = new PgpDecryptVerifyInputParcel(signedText);

        PgpDecryptVerifyOperation decryptVerifyOperation = new PgpDecryptVerifyOperation(context, providerHelper, null);
        DecryptVerifyResult result = decryptVerifyOperation.execute(pgpDecryptVerifyInputParcel, null);

        assertTrue(result.success());
        assertEquals(OpenPgpSignatureResult.RESULT_VALID_KEY_UNCONFIRMED, result.getSignatureResult().getResult());
        assertEquals(ring.getMasterKeyId(), result.getSignatureResult().getKeyId());
    }

    @Test
    public void testCreateEddsa() throws Exception {
        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Algorithm.EDDSA, 0, null, KeyFlags.CERTIFY_OTHER, 0L));
        parcel.mAddUserIds.add("ed");

        PgpKeyOperation op = new PgpKeyOperation(null);
        PgpEditKeyResult result = op.createSecretKeyRing(parcel);

        assertTrue("initial test key creation must succeed", result.success());
        assertNotNull("initial test key creation must succeed", result.getRing());

        CanonicalizedKeyRing canonicalizedKeyRing = result.getRing().canonicalize(new OperationLog(), 0);
        assertNotNull(canonicalizedKeyRing);
    }

    private UncachedKeyRing loadKeyringFromResource(String name) throws Exception {
        UncachedKeyRing ring = readRingFromResource(name);
        SaveKeyringResult saveKeyringResult = providerHelper.savePublicKeyRing(ring);
        assertTrue(saveKeyringResult.success());
        assertFalse(saveKeyringResult.getLog().containsWarnings());
        return ring;
    }

    private byte[] readBytesFromResource(String name) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream input = EddsaTest.class.getResourceAsStream(name);

        Util.copy(input, baos);

        return baos.toByteArray();
    }

    UncachedKeyRing readRingFromResource(String name) throws Exception {
        return UncachedKeyRing.fromStream(EddsaTest.class.getResourceAsStream(name)).next();
    }

}