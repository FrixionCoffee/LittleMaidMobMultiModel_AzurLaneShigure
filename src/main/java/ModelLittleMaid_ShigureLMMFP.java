import firis.lmavatar.common.modelcaps.PlayerModelCaps;
import firis.lmmm.api.caps.IModelCaps;
import firis.lmmm.api.renderer.ModelRenderer;
import firis.lmmm.builtin.model.ModelLittleMaid_SR2;
import net.blacklab.lmr.entity.littlemaid.EntityLittleMaid;
import net.blacklab.lmr.util.ModelCapsLittleMaid;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

/*
 * サンプルマルチモデル定義
 * <p>
 * ModelMultiBaseを継承したクラスをマルチモデルと判断する
 * <p>
 * 通常のメイドさんの改良
 * 　基準メイドさんを改変する場合はModelLittleMaidBaseを継承する
 * 　すでに存在するメイドさんを改変する場合はModelLittleMaid_SR2などを継承する
 * 　本クラスはSR2モデルを継承している
 * <p>
 * 命名規則（モデル）
 * 　ModelLittleMaid_xxxxxxxxをクラス名とすること
 * 　xxxxxxxxがモデル名となりテクスチャとのリンクに利用される
 * 　本クラスの場合はSampleMaidModelがモデル名となる
 * <p>
 * 命名規則（テクスチャ）
 * 　assets.minecraft.textures.entity.littleMaidが基準フォルダとなる
 * 　[パッケージ].[テクスチャ名]_[モデル名]形式でフォルダを作成する
 * 　　※[パッケージ]は省略可能
 * 　上記フォルダ内にメイドさんテクスチャを格納する
 * 　メイドさんテクスチャはxxxxxxxx_0.png～xxxxxxxx_f.pngまで
 * 　xxxxxxxxは任意の文字列となり末尾の英数字が16進数で16色の色情報を意味する
 * <p>
 * <p>
 * 本サンプルのテクスチャが格納されているフォルダは下記を意味している
 * 　Tutorial.DemoModel_SampleMaidModel
 * <p>
 * 　Tutorial [パッケージ]
 * 　DemoModel [テクスチャ名]
 * 　SampleMaidModel [モデル名]
 * <p>
 * 　[モデル名]はモデルクラスと同一の名前になるように設定すること
 * 　本サンプルの場合は[SampleMaidModel]がモデル名となる。
 * <p>
 * 同一モデルで別テクスチャを使用する場合は
 * 別名称のパッケージまたはテクスチャ名のフォルダを用意すれば
 * モデルは同じでテクスチャは別のものを設定できる
 * <p>
 * メイドさん選択画面でTutorial.DemoModel_SampleMaidModelが表示される
 */

public class ModelLittleMaid_ShigureLMMFP extends ModelLittleMaid_SR2 {
    public ModelRenderer skirtMasterMount;
    public List<SkirtVector> skirtVectors;

    public ModelRenderer mechanicalEarMasterMount;
    public ModelRenderer mechanicalEarRightMount;
    public ModelRenderer mechanicalEarLeftMount;

    public MechanicalEarHolder earLeftHolder;
    public MechanicalEarHolder earRightHolder;
    public ModelRenderer hairBand;
    public Hair hair;

    public ModelRenderer sailorRedRibbon;
    public ModelRenderer sailorSideRibbon;
    public ModelRenderer sailorNeck;

    public ModelRenderer sailorNeckBack;

    public ModelRenderer tailMasterMount;

    public MechanicalEarHolder tailHolder;

    public ModelRenderer skirtBelt;

    public ModelRenderer fishBombMasterMount;

    private static final Random random = new Random();

    /**
     * 確実なSinカーブが欲しい時に使うクラス
     * LMM FPのsetDefaultPoseにブレークポイント置いて見たときは毎Tick呼ばれない感じだったけど
     * アニメーション見る限り毎Tick呼ばれてる気がする。
     * <p>
     * 毎Tick呼ばれないとmh_sin(ageTick)で波が作れないので、強引に呼ばれた回数を条件に波の値を貰えるクラスを作成した。
     * 多分必要ない
     */
    @SuppressWarnings("unused")
    private static final class AnimationManager {
        private int callCounter;
        private static final float secondTicks = 20;
        private static final float pi = 3.151492f;
        private final List<Float> minecraftTicksSinCurves = new ArrayList<>();

        /**
         * 振幅は使用側でどうにかする。引数が小さければ小さいほど爆発的にメモリを食うので注意(2^-16くらいなら多分2Mb)
         *
         * @param frequencyUserValue 周波数。1.0fなら80tickを1周期。0.5fであれば160tickで1周期の波が作成される。2.0fなら40tick。2^-nなら80*n tick
         */
        AnimationManager(float frequencyUserValue) {
            callCounter = 0;
            int loopMax = (int) (1 / frequencyUserValue * 80);
            for (int i = 0; i <= loopMax; i++) {
                final float rad = i * pi / 2;
                final float frequency = 1 / secondTicks * frequencyUserValue;
                minecraftTicksSinCurves.add((float) Math.sin(rad * frequency));
            }

        }

        public float getTickCurveValue() {
            if (callCounter >= minecraftTicksSinCurves.size()) {
                callCounter = 1;
            }
            final float result = minecraftTicksSinCurves.get(callCounter);
            callCounter++;
            return result;
        }
    }

    /**
     * CapsHelperだけでは取得できないデータをリフレクションを用いて強引に取得するUtilクラス
     * あまり使用すべきでない
     */
    @SuppressWarnings("unused")
    private static final class ForceCapsFetcher {
        private static int cacheCalledOfNumber = 0;
        private static final Map<ModelCapsLittleMaid, EntityLittleMaid> cacheMap =
                Collections.synchronizedMap(new IdentityHashMap<>()); //同一インスタンスを保証するMap

        private static int playerCacheCalledOfNumber = 0;
        private static final Map<PlayerModelCaps, EntityPlayer> playerCacheMap =
                new IdentityHashMap<>();
        /**
         * @param entityCaps 対象のリトルメイドCaps
         * @return 契約残り日を少数切り捨ての自然数で返却
         */
        static Optional<Integer> getContractDays(final IModelCaps entityCaps) {

            Function<EntityLittleMaid, Integer> function = entityLittleMaid -> {
                final float contractDays = entityLittleMaid.getContractLimitDays();

                if (contractDays == -1f){
                    return null;
                }
                return ((int) contractDays);
            };

            return getLittleMaid(entityCaps).map(function);
        }

        /**
         *
         * @param entityCaps 対象のIModelCaps
         * @return メイドアバターを適用しているプレイヤーエンティティを返す
         */
        private static Optional<EntityPlayer> getPlayerLittleMaidAvatar(final IModelCaps entityCaps){
            try {
                final PlayerModelCaps playerModelCaps = (PlayerModelCaps)entityCaps;
                final EntityPlayer cache = playerCacheMap.get(playerModelCaps);

                if (cache != null){
                    return Optional.of(cache);
                }

                final Field field = playerModelCaps.getClass()
                        .getSuperclass()
                        .getDeclaredField("owner");
                field.setAccessible(true);
                final EntityPlayer entityPlayer = (EntityPlayer)field.get(playerModelCaps);

                if (!(entityPlayer instanceof EntityPlayerSP) | entityPlayer == null){
                    return Optional.empty();    // マルチでは動作させない
                }

                if (playerCacheMap.size() >=3 | playerCacheCalledOfNumber >= 64){
                    playerCacheMap.clear();
                    playerCacheCalledOfNumber = 0;
                }

                playerCacheMap.put(playerModelCaps, entityPlayer);
                playerCacheCalledOfNumber++;
                return Optional.of(entityPlayer);

            }catch (ClassCastException | NullPointerException | NoSuchFieldException | IllegalAccessException ignore){
                playerCacheMap.clear();
                return Optional.empty();

            }catch (SecurityException e){
                throw new SecurityException("お使いのJVMではリフレクションが動作しません", e);
            }
        }

        /**
         * setDefaultPauseに入ってくるModelCapsLittleMaidからリフレクションによるゴリ押しで対象のメイドを取得する関数
         * ModelCapsHelperだとentityはDeprecatedなのでリフレクションで取得した。そもそもModelCapsHelperはentity返さんくなったっぽい
         * <p>
         * 毎フレーム呼び出すと負荷がやばいと思う。
         * <p>
         * LMMやバージョンに強く依存しているため環境ごとに変更が必要(特にModelCaps付近)
         */
        private static Optional<EntityLittleMaid> getLittleMaid(final IModelCaps entityCaps) {

            final ModelCapsLittleMaid capsLittleMaid;
            try {
                capsLittleMaid = (ModelCapsLittleMaid) entityCaps;
                // 基本的にキャストに失敗することは無いと思うのでinstance ofで比較せず例外処理で任せる。正直どっちも誤差レベルらしいけど…
                // プレイヤーがメイドさんの場合は問答無用でClassCastException
            } catch (ClassCastException e) {
                return Optional.empty();
            }

            final EntityLittleMaid cacheCaps = cacheMap.get(capsLittleMaid);
            cacheCalledOfNumber++;

            if (cacheCaps != null) {
                // キャッシュにあればリフレクションは使わずに済む
                return Optional.of(cacheCaps);
            }

            if (capsLittleMaid.getClass() != ModelCapsLittleMaid.class) {
                // ModelCapsLittleMaidの子クラスを弾く
                return Optional.empty();
            }

            if (cacheMap.size() >= 32 || cacheCalledOfNumber >= 1024) {
                // 定期的(?)にキャッシュをワイプ
                cacheMap.clear();
                cacheCalledOfNumber = 0;
            }

            try {

                Field field = capsLittleMaid.getClass()
                        .getSuperclass()
                        .getDeclaredField("owner");

                field.setAccessible(true);
                EntityLittleMaid entityLittleMaid = (EntityLittleMaid) field.get(capsLittleMaid);
                cacheMap.put(capsLittleMaid, entityLittleMaid);
                return Optional.of(entityLittleMaid);

            } catch (NoSuchFieldException | IllegalAccessException | ClassCastException | NullPointerException e) {
                cacheMap.clear();
                return Optional.empty();

            } catch (SecurityException e) {
                throw new SecurityException("お使いのJVMではリフレクションが動作しません", e);
            }

        }
    }

    /**
     * null排除用のリスト
     * <p>
     * リトルメイドのマルチモデルは他クラスだとどうにも処理がうまく行かないっぽい
     * しかし、staticインナークラスに委譲する分は問題ないっぽい
     * <br><br>
     * インターフェースのデフォルト実装でもゴリ押し委譲できる辺り、多分クラスに所属している必要がある感じ
     * インナーは外部クラスに所属してるし、インターフェースはクラスの一部そのものだし…
     */
    private static final class NonNullList<E> extends ArrayList<E> {
        @Override
        public boolean add(E e) {
            if (e == null) {
                throw new IllegalArgumentException("ListElement is Null");
            }
            return super.add(e);
        }
    }

    /**
     * スカートの方向東西南北のModelRenderを保管するクラス
     * 外部が持っているMaster Mountを弄くればスカート全体を動かせるようになっている<br><br>
     * <p>
     * 外部フィールドのModelRendererもstaticインナーに仕舞う分は問題なさげ
     */
    private static final class SkirtVector {
        public final ModelRenderer vectorMount;
        public final List<ModelRenderer> vectorChildList;

        /**
         * @param vectorMount 各方向のスカートを制御するためのマウントパーツ
         */
        SkirtVector(ModelRenderer vectorMount) {
            this.vectorMount = vectorMount;
            vectorChildList = new NonNullList<>();
        }
    }

    /**
     * メカミミのパーツを保持するクラス
     * その場その場でクラスとか作ってたら改修できなくなった…
     */
    private static final class MechanicalEarHolder {
        public static final int widthX = 24;
        public static final int heightY = 29;
        public static final int sideHeightY = heightY + 2;

        public final List<ModelRenderer> mechanicalEars;
        public ArrayList<ModelRenderer> localPartsAccessList;   // local変数のModelRendererが参照切れしないように保持するリスト
        public boolean parentRegistered;

        public MechanicalEarHolder() {
            mechanicalEars = new NonNullList<>();
            localPartsAccessList = new NonNullList<>();
            parentRegistered = false;
        }

        public void registrationAllParent(final ModelRenderer earMount) {
            if (parentRegistered) {
                throw new UnsupportedOperationException("registerAllParent() call is 1 only");
            }

            for (ModelRenderer ear : mechanicalEars
            ) {
                earMount.addChild(ear);
            }
            parentRegistered = true;
        }

    }

    private static final class Hair {
        public ModelRenderer frontHair;
    }

    @SuppressWarnings("unused")
    public ModelLittleMaid_ShigureLMMFP() {
        super();
    }

    @SuppressWarnings("unused")
    public ModelLittleMaid_ShigureLMMFP(float psize) {
        super(psize, 1.35f * 0.9f, 256, 256);
    }

    @SuppressWarnings("unused")
    public ModelLittleMaid_ShigureLMMFP(float psize, float pyoffset, int pTextureWidth, int pTextureHeight) {
        super(psize, pyoffset, pTextureWidth, pTextureHeight);
    }

    @Override
    public void initModel(float size, float yOffset) {
        super.initModel(size, yOffset);
        initSkirt();
        initAllEar();
        initRibbon();

        initSideRibbon();
        initTailMethod();
        initSailorNeck();

        initSkirtBelt();

        fishBombMasterMount = new ModelRenderer(this);
        fishBombMasterMount.setRotationPoint(0, -30f, 0);
        bipedBody.addChild(fishBombMasterMount);
    }

    private void initSkirtBelt() {
        final float commonScale = 1.125f;
        skirtBelt = new ModelRenderer(this, 0, 80, commonScale, 0.66f, commonScale)
                .addBox(0, 0, 0, 6, 1, 4)
                .setRotationPoint(-3.30f, -2.5f, -2.2f);
        Skirt.addChild(skirtBelt);
    }

    private void initSailorNeck() {
        float commonScale = 0.1f;
        sailorNeck = new ModelRenderer(this, 1, 209, commonScale + 0.025f, commonScale, commonScale)
                .addPlate(0, 0, 0, 46, 14, 0)
                .setRotationPoint(-2.8f, 0, -2.1f);
        bipedBody.addChild(sailorNeck);

        commonScale = 0.15f;
        sailorNeckBack = new ModelRenderer(this, 1, 225, commonScale, commonScale + 0.025f, commonScale)
                .addPlate(0, 0, 0, 46, 14, 0)
                .setRotationPoint(-3.5f, 0, 2.1f);
        sailorNeckBack.setRotateAngleDegX(3);
        bipedBody.addChild(sailorNeckBack);
    }

    private void initRibbon() {
        final float ribbonScale = 0.085f;
        sailorRedRibbon = new ModelRenderer(this, 5, 176, ribbonScale, ribbonScale, ribbonScale);
        sailorRedRibbon.addPlate(0, 0, 0, 30, 30, 0)
                .setRotationPoint(-1.5f, 1, -2.02f);
        bipedBody.addChild(sailorRedRibbon);
    }

    private void initSideRibbon() {
        final float sideRibbonScale = 0.05f;
        sailorSideRibbon = new ModelRenderer(this, 51, 184, sideRibbonScale, sideRibbonScale, sideRibbonScale);
        sailorSideRibbon.addPlate(0, 0, 0, 51, 21, 0)
                .setRotationPoint(2.9f, 2.70f, 1)
                .setRotateAngleDeg(30, 90, 0);
        bipedBody.addChild(sailorSideRibbon);
    }

    private void initTailMethod() {
        tailMasterMount = new ModelRenderer(this)
                .setRotationPoint(12, 10f, 33.9f)
                .setRotateAngleDeg(90 + 19, 180, 0);
        Skirt.addChild(tailMasterMount);

        tailHolder = new MechanicalEarHolder();
        initTail(tailHolder, tailMasterMount);
    }

    private void initAllEar() {
        mechanicalEarMasterMount = new ModelRenderer(this);
        mechanicalEarMasterMount.setRotationPoint(-14.0f, -23.9f, -3.0f);
        mechanicalEarRightMount = new ModelRenderer(this);
        mechanicalEarLeftMount = new ModelRenderer(this);
        mechanicalEarLeftMount.setRotationPoint(3.75f, 0, 0);

        earLeftHolder = new MechanicalEarHolder();
        HeadTop.addChild(mechanicalEarMasterMount);
        mechanicalEarMasterMount.addChild(mechanicalEarRightMount);
        mechanicalEarMasterMount.addChild(mechanicalEarLeftMount);

        earRightHolder = new MechanicalEarHolder();
        initEar(earRightHolder, mechanicalEarRightMount);
        initEar(earLeftHolder, mechanicalEarLeftMount);


        hairBand = new ModelRenderer(this, 1, 74, 1, 0.33f, 1);
        hairBand.addBox(0, 0, 0, 5, 1, 1);
        hairBand.setRotationPoint(-2.5f, 4.71f, -2.1f);
        hairBand.setRotateAngleDegX(-6);
        HeadTop.addChild(hairBand);
    }

    private void initEar(MechanicalEarHolder holder, ModelRenderer mount) {
        float commonScale = 0.12f;
        for (int i = 0; i < 20; i++) {
            final int earTextureY;
            if (i >= 6 && i <= 10) {
                earTextureY = 74;
            } else if (i == 11) {
                earTextureY = 108;

            } else if (i == 19) {
                earTextureY = 142;
            } else {
                earTextureY = 42;
            }

            final float rebirthCommonScale = 1 - commonScale;

            ModelRenderer ear = new ModelRenderer(this, 24, earTextureY, commonScale, commonScale, commonScale);
            ear.addBox(0, 0, 0, MechanicalEarHolder.widthX, MechanicalEarHolder.heightY, 1);
            ear.setRotationPoint(MechanicalEarHolder.widthX / 2.0f * rebirthCommonScale, MechanicalEarHolder.heightY * rebirthCommonScale, 0.05f * i);
            holder.mechanicalEars.add(ear);


            final int sideEarTextureX = 76;
            final int sideEarTextureY = 40;
            final float sideEarDegX = 20;
            final float sideEarDegY = 90;


            ModelRenderer earLeft = new ModelRenderer(this, sideEarTextureX, sideEarTextureY, commonScale, commonScale, commonScale);
            earLeft.addPlate(0, 0, -0, 1, MechanicalEarHolder.sideHeightY, 0);// オフセット値は回転による影響を受ける
            earLeft.setRotationPoint(13 * commonScale, 0, 1 * commonScale);    // ポジション値は回転による影響を受けないため感覚で設置可能
            earLeft.setRotateAngleDeg(sideEarDegX, sideEarDegY, 0);
            holder.localPartsAccessList.add(earLeft);
            ear.addChild(earLeft);

            ModelRenderer earRight = new ModelRenderer(this, sideEarTextureX, sideEarTextureY, commonScale, commonScale, commonScale);
            earRight.addPlate(0, 0, 0, 1, MechanicalEarHolder.sideHeightY, 0);
            earRight.setRotationPoint(11 * commonScale, 0, 1 * commonScale);
            earRight.setRotateAngleDeg(-sideEarDegX, sideEarDegY, 0);
            ear.addChild(earRight);
            holder.localPartsAccessList.add(earRight);

            commonScale *= 0.95f;
        }
        holder.registrationAllParent(mount);
    }

    private void initTail(MechanicalEarHolder holder, ModelRenderer mount) {
        float commonScale = 0.18f;

        for (int i = 0; i < 32; i++) {
            final int tailTextureY;
            final int tailTextureX = 80;
            final int sideTailTextureX = 132;
            final int sideTailTextureY;
            if (i <= 5) {
                tailTextureY = 42;
                sideTailTextureY = 40;
            } else {
                tailTextureY = 74;
                sideTailTextureY = 72;
            }

            final float rebirthCommonScale = 1 - commonScale;

            ModelRenderer tail = new ModelRenderer(this, tailTextureX, tailTextureY, commonScale, commonScale * 2, commonScale);
            tail.addBox(0, 0, 0, MechanicalEarHolder.widthX, MechanicalEarHolder.heightY, 1);
            tail.setRotationPoint(MechanicalEarHolder.widthX / 2.0f * rebirthCommonScale, MechanicalEarHolder.heightY * rebirthCommonScale, 0.05f * i);
            holder.mechanicalEars.add(tail);


            final float sideEarDegX = 10;
            final float sideEarDegY = 90;


            ModelRenderer tailLeft = new ModelRenderer(this, sideTailTextureX, sideTailTextureY, commonScale, commonScale * 2, commonScale);
            tailLeft.addPlate(0, 0, -0, 1, MechanicalEarHolder.sideHeightY - 1, 0);// オフセット値は回転による影響を受ける
            tailLeft.setRotationPoint(13 * commonScale, 0, 1 * commonScale);    // ポジション値は回転による影響を受けないため感覚で設置可能
            tailLeft.setRotateAngleDeg(sideEarDegX, sideEarDegY, 0);
            holder.localPartsAccessList.add(tailLeft);
            tail.addChild(tailLeft);

            ModelRenderer tailRight = new ModelRenderer(this, sideTailTextureX, sideTailTextureY, commonScale, commonScale * 2, commonScale);
            tailRight.addPlate(0, 0, 0, 1, MechanicalEarHolder.sideHeightY - 1, 0);
            tailRight.setRotationPoint(11 * commonScale, 0, 1 * commonScale);
            tailRight.setRotateAngleDeg(-sideEarDegX, sideEarDegY, 0);
            tail.addChild(tailRight);
            holder.localPartsAccessList.add(tailRight);

            commonScale *= 0.95f;
        }
        holder.registrationAllParent(mount);
    }

    private void initSkirt() {
        skirtMasterMount = new ModelRenderer(this);
        skirtMasterMount.setRotationPoint(-3, -2.3f, -0.51f);
        Skirt.addChild(skirtMasterMount);
        skirtVectors = new NonNullList<>();

        for (int i = 0; i <= 3; i++) {

            final ModelRenderer vectorMountParts = new ModelRenderer(this);
            skirtVectors.add(new SkirtVector(vectorMountParts));
            skirtMasterMount.addChild(vectorMountParts);
        }

        assert skirtVectors.size() == 4;

        for (SkirtVector s : skirtVectors
        ) {
            assert s.vectorChildList.isEmpty();
        }

        int outLoopIndex = 0;
        for (final SkirtVector skirtVector : skirtVectors
        ) {
            final float scaleX;
            if (outLoopIndex % 2 == 0) {
                scaleX = 0.096f;
            } else {
                scaleX = 0.065f;
            }

            for (int i = 0; i < 5; i++) {
                final float movedPointZ;
                final float movedPointY;
                final int textureY;

                if (i % 2 == 0) {
                    movedPointZ = 0.1f;
                    movedPointY = 0.45f;
                    textureY = 42;
                } else {
                    movedPointZ = 0.0f;
                    movedPointY = 0.0f;
                    textureY = 56;
                }

                ModelRenderer skirtParts = new ModelRenderer(this, 0, textureY, scaleX, 0.2f, 1.0f)
                        .addPlate(14.28f * i, movedPointY, -movedPointZ, 24, 14, 0);
                skirtVector.vectorChildList.add(skirtParts);
                skirtVector.vectorMount.addChild(skirtParts);
            }

            final ModelRenderer mount = skirtVector.vectorMount;
            final float commonSideSkirtDeg = 13.0f;
            switch (outLoopIndex) {
                case 0:
                    mount.setRotateAngleDegX(-commonSideSkirtDeg);
                    mount.setRotationPoint(-1, 0, -1.53f);
                    break;

                case 1:
                    mount.setRotateAngleDegX(commonSideSkirtDeg);
                    mount.setRotateAngleDegY(90);
                    mount.setRotationPoint(6.1f, 0, 3);// スカート左
                    break;

                case 2:
                    mount.setRotateAngleDegX(commonSideSkirtDeg);
                    mount.setRotationPoint(-1, 0, 2.51f);// スカート後ろ
                    break;

                case 3:
                    mount.setRotateAngleDegX(-commonSideSkirtDeg);
                    mount.setRotateAngleDegY(90);
                    mount.setRotationPoint(-0.1f, 0, 3);// スカート右
                    break;

                default:
                    throw new IllegalStateException();

            }
            outLoopIndex++;
        }
    }

    @Override
    public void setDefaultPause(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, IModelCaps entityCaps) {
        super.setDefaultPause(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityCaps);

        // LMM FPだとsetDefaultPauseでアニメーションさせる必要があるっぽい？。とりあえず何故か動くからヨシ！

        final float amplitude = 0.5f;   // 振幅
        final float secondTicks = 128.0f;
        final float rad = 3.1514592f * ageInTicks / 2.0f; // 周期π/2ならsinは1
        final float frequency = 1.0f / secondTicks;    // 周波数
        final float defaultRotateAngleDegX = 90 + 19;
        try {
            tailMasterMount.setRotateAngleDegX(defaultRotateAngleDegX + amplitude * mh_sin(rad * frequency));

        } catch (ArithmeticException ignored) {

        }
    }

    @Override
    public void setLivingAnimations(IModelCaps entityCaps, float limbSwing, float limbSwingAmount, float partialTickTime) {
        super.setLivingAnimations(entityCaps, limbSwing, limbSwingAmount, partialTickTime);

        if (random.nextInt(1200) == 1111){
            final Potion happinessPotion = Potion.getPotionById(26);
            if (happinessPotion == null){
                return;
            }
            
            ForceCapsFetcher.getLittleMaid(entityCaps).ifPresent(entityLittleMaid -> {
                entityLittleMaid.addPotionEffect(new PotionEffect(happinessPotion, 2400, 0));
            });
            ForceCapsFetcher.getPlayerLittleMaidAvatar(entityCaps).ifPresent(entityPlayer -> {
                entityPlayer.addPotionEffect(new PotionEffect(happinessPotion, 2400, 0));
            });
        }

    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, IModelCaps entityCaps) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityCaps);

    }


}
