package com.dasisiheung.service;

import com.dasisiheung.dto.option.OptionItem;
import com.dasisiheung.dto.option.OptionsResponse;
import com.dasisiheung.dto.space.SpaceSort;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.entity.application.OfferSide;
import com.dasisiheung.entity.application.PhotoPhase;
import com.dasisiheung.entity.application.PriceNegotiationStatus;
import com.dasisiheung.entity.community.PostCategory;
import com.dasisiheung.entity.faq.FaqCategory;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.entity.report.ReportAction;
import com.dasisiheung.entity.report.ReportReason;
import com.dasisiheung.entity.report.ReportStatus;
import com.dasisiheung.entity.report.ReportTargetType;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.BuildingUse;
import com.dasisiheung.entity.space.CommercialTheme;
import com.dasisiheung.entity.space.Facility;
import com.dasisiheung.entity.space.ProofType;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.ReviewDecision;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.entity.space.UsePurpose;
import org.springframework.stereotype.Service;

/** enum 에서 선택지를 만들어 한 번만 계산해 둔다. (코드 배포 전에는 바뀌지 않음) */
@Service
public class OptionService {

    private final OptionsResponse options = new OptionsResponse(
            OptionItem.listOf(Role.class, Role::isSelfRegistrable),
            OptionItem.listOf(Region.class),
            OptionItem.listOf(ActivityType.class),
            OptionItem.listOf(Facility.class),
            OptionItem.listOf(BuildingUse.class),
            OptionItem.listOf(CommercialTheme.class),
            OptionItem.listOf(UsePurpose.class),
            OptionItem.listOf(ProofType.class),
            OptionItem.listOf(SpaceStatus.class),
            OptionItem.listOf(ReviewDecision.class),
            OptionItem.listOf(SpaceSort.class),
            OptionItem.listOf(ApplicationStatus.class),
            OptionItem.listOf(PriceNegotiationStatus.class),
            OptionItem.listOf(OfferSide.class),
            OptionItem.listOf(PhotoPhase.class),
            OptionItem.listOf(PostCategory.class),
            OptionItem.listOf(ReportTargetType.class),
            OptionItem.listOf(ReportReason.class),
            OptionItem.listOf(ReportStatus.class),
            OptionItem.listOf(ReportAction.class),
            OptionItem.listOf(FaqCategory.class)
    );

    public OptionsResponse getOptions() {
        return options;
    }
}
