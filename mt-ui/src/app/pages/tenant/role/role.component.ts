import { AfterViewInit, ChangeDetectorRef, Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { MatBottomSheetRef, MAT_BOTTOM_SHEET_DATA } from '@angular/material/bottom-sheet';
import { TranslateService } from '@ngx-translate/core';
import { FormInfoService } from 'mt-form-builder';
import { combineLatest, of } from 'rxjs';
import { switchMap, take } from 'rxjs/operators';
import { Aggregate } from 'src/app/clazz/abstract-aggregate';
import { IBottomSheet, ISumRep } from 'src/app/clazz/summary.component';
import { IProjectSimple } from 'src/app/clazz/validation/aggregate/project/interface-project';
import { RoleValidator } from 'src/app/clazz/validation/aggregate/role/validator-role';
import { ErrorMessage } from 'src/app/clazz/validation/validator-common';
import { FORM_CONFIG } from 'src/app/form-configs/role.config';
import { INewRole } from 'src/app/pages/tenant/my-roles/my-roles.component';
import { EndpointService } from 'src/app/services/endpoint.service';
import { MyRoleService } from 'src/app/services/my-role.service';
import { MyPermissionService } from 'src/app/services/my-permission.service';
import { ProjectService } from 'src/app/services/project.service';
import { IQueryProvider } from 'mt-form-builder/lib/classes/template.interface';
import { HttpProxyService } from 'src/app/services/http-proxy.service';
@Component({
  selector: 'app-role',
  templateUrl: './role.component.html',
  styleUrls: ['./role.component.css']
})
export class RoleComponent extends Aggregate<RoleComponent, INewRole> implements OnInit, AfterViewInit, OnDestroy {
  bottomSheet: IBottomSheet<INewRole>;
  public loadRoot =undefined;
  public loadChildren ;
  public permissionFg: FormGroup = new FormGroup({})
  public apiRootId: string;
  constructor(
    public entitySvc: MyRoleService,
    public epSvc: EndpointService,
    public permissoinSvc: MyPermissionService,
    public projectSvc: ProjectService,
    public httpProxySvc: HttpProxyService,
    fis: FormInfoService,
    @Inject(MAT_BOTTOM_SHEET_DATA) public data: any,
    bottomSheetRef: MatBottomSheetRef<RoleComponent>,
    cdr: ChangeDetectorRef,
    private translate: TranslateService
  ) {
    super('role-form', JSON.parse(JSON.stringify(FORM_CONFIG)), new RoleValidator(), bottomSheetRef, data, fis, cdr)
    this.bottomSheet = data;
    this.permissoinSvc.setProjectId(this.bottomSheet.params['projectId'])

    this.entitySvc.setProjectId(this.bottomSheet.params['projectId'])
    this.fis.queryProvider[this.formId + '_' + 'parentId'] = this.getParents();

    this.loadRoot = this.permissoinSvc.readEntityByQuery(0, 1000, "parentId:null");
    this.loadChildren = (id: string) => {
      return this.permissoinSvc.readEntityByQuery(0, 1000, "parentId:" + id)
    }
    this.fis.formCreated(this.formId).pipe(take(1)).subscribe(() => {
      if (this.bottomSheet.context === 'new') {
        this.fis.formGroupCollection[this.formId].get('projectId').setValue(this.bottomSheet.params['projectId'])
      }
    })
  }
  getParents():IQueryProvider {
    return {
      readByQuery: (num: number, size: number, query?: string, by?: string, order?: string, header?: {}) => {
        return this.httpProxySvc.readEntityByQuery<INewRole>(this.entitySvc.entityRepo, num, size, `types:PROJECT.USER`, by, order, header)
      }
    } as IQueryProvider
  }
  ngAfterViewInit(): void {
    if (this.bottomSheet.context === 'edit') {
      this.fis.formGroupCollection[this.formId].get('id').setValue(this.aggregate.id)
      this.fis.formGroupCollection[this.formId].get('name').setValue(this.aggregate.name)
      this.fis.formGroupCollection[this.formId].get('description').setValue(this.aggregate.description ? this.aggregate.description : '')
      this.fis.formGroupCollection[this.formId].get('projectId').setValue(this.aggregate.projectId)
      this.aggregate.permissionIds.forEach(p => {
        if (!this.permissionFg.get(p)) {
          this.permissionFg.addControl(p, new FormControl('checked'))
        } else {
          this.permissionFg.get(p).setValue('checked', { emitEvent: false })
        }
      })
      this.cdr.markForCheck()
    }
  }
  ngOnDestroy(): void {
    Object.keys(this.subs).forEach(k => { this.subs[k].unsubscribe() })
    this.fis.resetAllExcept(['summaryRoleCustomerView'])
  }
  ngOnInit() {
  }
  convertToPayload(cmpt: RoleComponent): INewRole {
    let formGroup = cmpt.fis.formGroupCollection[cmpt.formId];
    const value = cmpt.permissionFg.value
    return {
      id: formGroup.get('id').value,//value is ignored
      name: formGroup.get('name').value,
      parentId: formGroup.get('parentId').value,
      projectId: formGroup.get('projectId').value,
      permissionIds: Object.keys(value).filter(e => value[e] === 'checked').filter(e=>e),
      description: formGroup.get('description').value ? formGroup.get('description').value : null,
      version: cmpt.aggregate && cmpt.aggregate.version
    }
  }
  update() {
    if (this.validateHelper.validate(this.validator, this.convertToPayload, 'update', this.fis, this, this.errorMapper))
      this.entitySvc.update(this.aggregate.id, this.convertToPayload(this), this.changeId)
  }
  create() {
    if (this.validateHelper.validate(this.validator, this.convertToPayload, 'create', this.fis, this, this.errorMapper)) {
      this.entitySvc.create(this.convertToPayload(this), this.changeId)
    }
  }
  errorMapper(original: ErrorMessage[], cmpt: RoleComponent) {
    return original.map(e => {
      return {
        ...e,
        formId: cmpt.formId
      }
    })
  }
}
