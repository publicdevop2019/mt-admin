import { Overlay, OverlayConfig } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { Component, OnDestroy } from '@angular/core';
import { MatBottomSheet } from '@angular/material/bottom-sheet';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { FormInfoService } from 'mt-form-builder';
import { IForm } from 'mt-form-builder/lib/classes/template.interface';
import { Observable } from 'rxjs';
import { filter } from 'rxjs/operators';
import { SummaryEntityComponent } from 'src/app/clazz/summary.component';
import { ObjectDetailComponent } from 'src/app/components/object-detail/object-detail.component';
import { ISearchConfig } from 'src/app/components/search/search.component';
import { FORM_CONFIG } from 'src/app/form-configs/stored-event.config';
import { DeviceService } from 'src/app/services/device.service';
import { OverlayService } from 'src/app/services/overlay.service';
import { IStoredEvent, StoredEventService } from 'src/app/services/stored-event.service';
import { IBizTask } from 'src/app/services/task.service';
@Component({
  selector: 'app-summary-stored-event',
  templateUrl: './summary-stored-event.component.html',
  styleUrls: ['./summary-stored-event.component.css']
})
export class SummaryStoredEventComponent extends SummaryEntityComponent<IStoredEvent, IStoredEvent> implements OnDestroy {
  formId = 'summaryStoredEvent';
  formInfo: IForm = JSON.parse(JSON.stringify(FORM_CONFIG));
  private formCreatedOb: Observable<string>;
  displayedColumns: string[] = ['id', 'eventBody', 'timestamp', 'domainId', 'name', 'internal','retry'];
  searchConfigs: ISearchConfig[] = [
    {
      searchLabel: 'ID',
      searchValue: 'id',
      type: 'text',
      multiple: {
        delimiter:'.'
      }
    },
    {
      searchLabel: 'REFERENCE_ID',
      searchValue: 'domainId',
      type: 'text',
      multiple: {
        delimiter:'.'
      }
    }
  ]
  constructor(
    public entitySvc: StoredEventService,
    public deviceSvc: DeviceService,
    public bottomSheet: MatBottomSheet,
    public dialog: MatDialog,
    private overlay: Overlay,
    private overlaySvc: OverlayService,
    private fis: FormInfoService,
    ) {
      super(entitySvc, deviceSvc, bottomSheet, 1);
      this.formCreatedOb = this.fis.formCreated(this.formId);
      this.formCreatedOb.subscribe(()=>{
        this.fis.formGroupCollection[this.formId].setValue({appName:entitySvc.getServiceName()})
        this.fis.formGroupCollection[this.formId].valueChanges.subscribe(e=>{
          entitySvc.setServiceName(e.appName);
          entitySvc.pageNumber=0;
        this.deviceSvc.refreshSummary.next();
      })
    })
  }
  launchOverlay(el: MatIcon, data: IBizTask) {
    this.overlaySvc.data = data;
    let config = new OverlayConfig();
    config.hasBackdrop = true;
    config.positionStrategy = this.overlay.position().global().centerVertically().centerHorizontally();
    config.scrollStrategy = this.overlay.scrollStrategies.reposition();
    const overlayRef = this.overlay.create(config);
    const filePreviewPortal = new ComponentPortal(ObjectDetailComponent);
    overlayRef.attach(filePreviewPortal);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    })

  }
  doRetry(id:string){
    this.entitySvc.retry(id).subscribe()
  }
}