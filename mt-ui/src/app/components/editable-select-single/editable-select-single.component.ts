import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { IEditEvent } from '../editable-field/editable-field.component';
import { IOption } from 'src/app/misc/interface';
@Component({
  selector: 'app-editable-select-single',
  templateUrl: './editable-select-single.component.html',
  styleUrls: ['./editable-select-single.component.css']
})
export class EditableSelectSingleComponent implements OnInit {
  @Input() inputValue: IOption = undefined;
  @Input() list: IOption[] = [];
  @Input() readOnly: boolean = false;
  @Output() newValue: EventEmitter<IEditEvent> = new EventEmitter();
  displayEdit = 'hidden';
  lockEditIcon = false;
  constructor() {
  }

  ngOnInit() {
  }
  showEditIcon() {
    this.displayEdit = 'visible'
  }
  hideEditIcon() {
    this.displayEdit = 'hidden'
  }
  doCancel() {
    this.displayEdit = 'hidden';
  }
  doUpdate(newValue: IOption) {
    this.newValue.emit({ original: (this.inputValue && this.inputValue.value as string), next: (newValue.value as string) });
    this.displayEdit = 'hidden';
  }
}
